package learn.queuesystem.infra.repository;

import learn.queuesystem.domain.queue.RedisPrefix;
import learn.queuesystem.domain.queue.WaitingQueueRepository;
import learn.queuesystem.infra.redis.QueueKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
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
    private static final String V2_TOKEN_EXP_KEY = "queue:v2:token:exp";
    private static final DefaultRedisScript<Long> ENTER_QUEUE_SCRIPT = new DefaultRedisScript<>(
            """
            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])
            local rank = redis.call('ZRANK', KEYS[1], ARGV[2])
            redis.call('SET', KEYS[2], ARGV[3], 'EX', ARGV[4])
            redis.call('SADD', KEYS[3], ARGV[5])
            redis.call('ZADD', KEYS[4], ARGV[6], ARGV[7])
            return rank
            """,
            Long.class
    );
    private static final DefaultRedisScript<List> STATUS_BY_TOKEN_SCRIPT = new DefaultRedisScript<>(
            """
            local tokenValue = redis.call('GET', KEYS[1])
            if not tokenValue then
                return {'EXPIRED', '-1'}
            end

            local userUuid, contentId = string.match(tokenValue, '([^|]+)|([^|]+)')
            if not userUuid or not contentId then
                return {'EXPIRED', '-1'}
            end

            local activeZSetKey = ARGV[1] .. contentId
            local activeExpireAt = redis.call('ZSCORE', activeZSetKey, userUuid)
            local nowMillis = tonumber(ARGV[3])
            if activeExpireAt and tonumber(activeExpireAt) > nowMillis then
                return {'DONE', '0'}
            end

            local waitKey = ARGV[2] .. contentId
            local rank = redis.call('ZRANK', waitKey, userUuid)
            if not rank then
                return {'EXPIRED', '-1'}
            end
            return {'WAIT', tostring(rank + 1)}
            """,
            List.class
    );

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
    public Long enterQueueAtomically(String waitKey, String tokenKey, String tokenValue, String contentId, String userUuid, long nowMillis, int tokenTtlSeconds) {
        long tokenExpireAt = nowMillis + (tokenTtlSeconds * 1000L);
        String token = tokenKey.substring(tokenKey.lastIndexOf(':') + 1);
        List<String> keys = List.of(waitKey, tokenKey, ACTIVE_CONTENTS.getKey(), V2_TOKEN_EXP_KEY);
        return redisTemplate.execute(
                ENTER_QUEUE_SCRIPT,
                keys,
                String.valueOf(nowMillis),
                userUuid,
                tokenValue,
                String.valueOf(tokenTtlSeconds),
                contentId,
                String.valueOf(tokenExpireAt),
                token
        );
    }

    @Override
    public void issueEnterTicketsInPipeline(String contentId, Set<String> userIds, int ttlSeconds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        long expireAt = System.currentTimeMillis() + (ttlSeconds * 1000L);
        String activeTicketZSetKey = QueueKeyGenerator.activeTicketZsetKey(contentId);

        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) {
                for (String userId : userIds) {
                    operations.opsForZSet().add(activeTicketZSetKey, userId, expireAt);
                }
                return null;
            }
        });
    }

    @Override
    public List<String> findQueueStatusByToken(String tokenKey) {
        String token = tokenKey.substring(tokenKey.lastIndexOf(':') + 1);
        List<String> keys = List.of(tokenKey);
        List<?> result = redisTemplate.execute(
                STATUS_BY_TOKEN_SCRIPT,
                List.of(tokenKey, V2_TOKEN_EXP_KEY),
                QueueKeyGenerator.activeTicketZsetKey(""),
                WAIT_QUEUE.getKey(),
                String.valueOf(System.currentTimeMillis()),
                token
        );
        if (result == null || result.size() < 2) {
            return List.of("EXPIRED", "-1");
        }
        return List.of(String.valueOf(result.get(0)), String.valueOf(result.get(1)));
    }

    @Override
    public long countActiveTicketsV2(String contentId, long nowMillis) {
        String key = QueueKeyGenerator.activeTicketZsetKey(contentId);
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, nowMillis);
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size == null ? 0 : size;
    }

    @Override
    public long countActiveTokensV2(long nowMillis) {
        redisTemplate.opsForZSet().removeRangeByScore(V2_TOKEN_EXP_KEY, 0, nowMillis);
        Long size = redisTemplate.opsForZSet().zCard(V2_TOKEN_EXP_KEY);
        return size == null ? 0 : size;
    }
}
