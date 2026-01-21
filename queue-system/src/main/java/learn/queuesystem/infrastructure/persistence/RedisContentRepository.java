package learn.queuesystem.infrastructure.persistence;

import learn.queuesystem.domain.ticket.ContentRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class RedisContentRepository implements ContentRepository {

    private static final String ACTIVE_CONTENTS_KEY = "queue:active_contents";
    private static final String WAIT_KEY_PREFIX = "queue:wait:";

    private final StringRedisTemplate redisTemplate;

    public RedisContentRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 컨텐츠를 활성 목록에 저장합니다.
     * 유저가 대기열에 처음 진입할 때 호출하여 스케줄러가 인지하게 합니다.
     */
    @Override
    public void saveContent(String contentId) {
        redisTemplate.opsForSet().add(WAIT_KEY_PREFIX, contentId);
    }

    /**
     * 현재 대기열이 돌아가고 있는 모든 컨텐츠 ID를 조회합니다.
     */
    public Set<String> getActiveContentIds() {
        return redisTemplate.opsForSet().members(ACTIVE_CONTENTS_KEY);
    }

    /**
     * 대기열이 종료된 컨텐츠를 목록에서 제거합니다.
     */
    public void removeContent(String contentId) {
        redisTemplate.opsForSet().remove(ACTIVE_CONTENTS_KEY, contentId);
    }
}
