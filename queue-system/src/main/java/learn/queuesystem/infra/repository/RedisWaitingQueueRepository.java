package learn.queuesystem.infra.repository;

import learn.queuesystem.domain.queue.RedisPrefix;
import learn.queuesystem.domain.queue.WaitingQueueRepository;
import learn.queuesystem.infra.redis.QueueKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static learn.queuesystem.domain.queue.RedisPrefix.*;

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
    public Long registerAndGetRank(String key, String memberId) {
        redisTemplate.opsForZSet().add(key, memberId, System.currentTimeMillis());
        return redisTemplate.opsForZSet().rank(key, memberId);
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
    public Long queueSize(String key) {
        Long size = redisTemplate.opsForZSet().size(key);
        return size == null ? 0 : size;
    }

    @Override
    public Set<String> getTopMembers(String key, long count) {
        Set<String> members = redisTemplate.opsForZSet().range(key, 0, count - 1);
        return members != null ? members : Collections.emptySet();
    }


    @Override
    public Set<String> getActiveContents() {
        Set<String> contents = redisTemplate.opsForSet()
                .members(ACTIVE_CONTENTS.getKey());
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
    public Boolean isActiveUser(String key) {
        return redisTemplate.opsForValue().get(key) != null;
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

    @Override
    public void issueEnterTicket(String key, int ttl) {
        redisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttl, TimeUnit.SECONDS);
    }


    /*
        입장 대기열 (set)을 추가합니다.
     */
    @Override
    public void activeContent(String contentId) {
        redisTemplate.opsForSet().add(ACTIVE_CONTENTS.getKey(), contentId);
    }

    @Override
    public void issueTokenKey(String key, String userUuidAndContentId, int ttl) {
        redisTemplate.opsForValue()
                .setIfAbsent(key, userUuidAndContentId, ttl, TimeUnit.SECONDS);
    }

    @Override
    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public long countKeysByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();

        Long count = redisTemplate.execute((RedisCallback<Long>) connection -> {
            long matched = 0L;
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    cursor.next();
                    matched++;
                }
            }
            return matched;
        });

        return count == null ? 0 : count;
    }
}
