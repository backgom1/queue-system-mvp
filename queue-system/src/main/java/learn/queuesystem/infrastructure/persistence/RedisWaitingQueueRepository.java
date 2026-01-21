package learn.queuesystem.infrastructure.persistence;

import learn.queuesystem.domain.queue.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisWaitingQueueRepository implements WaitingQueueRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String ACTIVE_CONTENTS_KEY = "waiting_contents";
    private static final String ACTIVE_EXPIRATION_KEY = "queue:active:expiration";
    private static final String DONE_COUNT_KEY = "queue:done:total";

    @Override
    public void register(String key, String member) {
        redisTemplate.opsForZSet().add(key, member, System.currentTimeMillis());
    }

    @Override
    public void remove(String key, String member) {
        redisTemplate.opsForZSet().remove(key, member);
    }

    @Override
    public Long getRank(String key, String member) {
        return redisTemplate.opsForZSet().rank(key, member);
    }

    @Override
    public Set<String> getTopMembers(String key, long count) {
        Set<String> members = redisTemplate.opsForZSet().range(key, 0, count - 1);
        return members != null ? members : Collections.emptySet();
    }


    @Override
    public void registerContent(String contentId) {
        redisTemplate.opsForSet().add(ACTIVE_CONTENTS_KEY, contentId);
    }

    @Override
    public Set<String> getActiveContents() {
        Set<String> contents = redisTemplate.opsForSet().members(ACTIVE_CONTENTS_KEY);
        return contents != null ? contents : Collections.emptySet();
    }

    @Override
    public void removeContent(String contentId) {
        redisTemplate.opsForSet().remove(ACTIVE_CONTENTS_KEY, contentId);
    }

    @Override
    public void addActiveToken(String userUuid, long expirationTimestamp) {
        redisTemplate.opsForZSet().add(ACTIVE_EXPIRATION_KEY, userUuid, (double) expirationTimestamp);
    }

    @Override
    public Set<String> popExpiredActiveTokens(long currentTimestamp) {
        Set<String> expiredMembers = redisTemplate.opsForZSet().rangeByScore(ACTIVE_EXPIRATION_KEY, 0, (double) currentTimestamp);
        if (expiredMembers != null && !expiredMembers.isEmpty()) {
            redisTemplate.opsForZSet().remove(ACTIVE_EXPIRATION_KEY, expiredMembers.toArray());
            return expiredMembers;
        }
        return Collections.emptySet();
    }

    @Override
    public long getQueueSize(String key) {
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size != null ? size : 0;
    }

    @Override
    public long getActiveTokenSize() {
        Long size = redisTemplate.opsForZSet().zCard(ACTIVE_EXPIRATION_KEY);
        return size != null ? size : 0;
    }

    @Override
    public Boolean hasActiveToken(String userUuid) {
        return redisTemplate.opsForZSet().score(ACTIVE_EXPIRATION_KEY, userUuid) != null;
    }

    @Override
    public void saveUserContent(String userUuid, String contentId) {
        redisTemplate.opsForValue().set("queue:user:" + userUuid, contentId);
    }

    @Override
    public Boolean hasUser(String userUuid) {
        return redisTemplate.hasKey("queue:user:" + userUuid);
    }

    @Override
    public String getUserContent(String userUuid) {
        return redisTemplate.opsForValue().get("queue:user:" + userUuid);
    }

    @Override
    public void incrementDoneCount(String contentId) {
        redisTemplate.opsForValue().increment(DONE_COUNT_KEY);
    }

    @Override
    public long getDoneCount() {
        String count = redisTemplate.opsForValue().get(DONE_COUNT_KEY);
        return count != null ? Long.parseLong(count) : 0;
    }

    @Override
    public Set<ZSetOperations.TypedTuple<String>> popMin(String key, int count) {
        return redisTemplate.opsForZSet().popMin(key, count);
    }
}