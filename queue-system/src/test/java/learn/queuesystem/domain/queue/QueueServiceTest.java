package learn.queuesystem.domain.queue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
class QueueServiceTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @org.junit.jupiter.api.BeforeEach
    void clearRedis() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("대기열 활성화 시 먼저 들어온 순서대로 N명만 진입 상태가 되어야 한다")
    void activateTokens_fifo() {
        // given: 10명의 대기자 생성
        int totalUsers = 10;
        String contentId = "concert-1";
        
        IntStream.rangeClosed(1, totalUsers).forEach(i -> {
            queueService.enterQueue(UUID.randomUUID().toString(), contentId);
            // 생성 시간 차이를 두기 위해 잠시 대기
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        });

        // when: 5명 활성화
        int activeCount = 5;
        queueService.activateTokens(activeCount);

        // then
        QueueService.QueueStats stats = queueService.getStats();
        
        assertThat(stats.proceeding()).isEqualTo(activeCount);
        assertThat(stats.waiting()).isEqualTo(totalUsers - activeCount);
    }
}