package learn.queuesystem.domain.queue;

import learn.queuesystem.application.service.QueueServiceV1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class QueueServiceV1Test {

    @Autowired
    private QueueServiceV1 queueServiceV1;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void clearRedis() {
        Assertions.assertNotNull(redisTemplate.getConnectionFactory());
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("대기열 활성화 시 먼저 들어온 순서대로 N명만 진입 상태가 되어야 한다")
    void activateTokens_fifo() {
        // given: 10명의 대기자 생성
        int totalUsers = 10;
        String contentId = "concert-1";

        IntStream.rangeClosed(1, totalUsers).forEach(i -> {
            queueServiceV1.enterQueue(UUID.randomUUID().toString(), contentId);
            // 생성 시간 차이를 두기 위해 잠시 대기
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        });

        // when: 5명 활성화
        int activeCount = 5;
        queueServiceV1.activateTokens(activeCount);

        // then
        QueueServiceV1.QueueStats stats = queueServiceV1.getStats();

        assertThat(stats.proceeding()).isEqualTo(activeCount);
        assertThat(stats.waiting()).isEqualTo(totalUsers - activeCount);
    }
}