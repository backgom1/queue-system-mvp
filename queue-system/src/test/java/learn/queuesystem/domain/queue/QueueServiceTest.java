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
class QueueServiceTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private QueueRepository queueRepository;

    @Test
    @DisplayName("대기열 활성화 시 먼저 들어온 순서대로 N명만 진입 상태가 되어야 한다")
    void activateTokens_fifo() {
        // given: 10명의 대기자 생성
        int totalUsers = 10;
        IntStream.rangeClosed(1, totalUsers).forEach(i -> {
            queueRepository.save(Queue.wait(UUID.randomUUID().toString(), "concert-1"));
            // 생성 시간 차이를 두기 위해 잠시 대기
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        });

        // when: 5명 활성화
        int activeCount = 5;
        queueService.activateTokens(activeCount);

        // then
        List<Queue> allQueues = queueRepository.findAll();
        
        long proceedCount = allQueues.stream()
            .filter(q -> q.getStatus() == QueueStatus.PROCEED)
            .count();
        
        long waitCount = allQueues.stream()
            .filter(q -> q.getStatus() == QueueStatus.WAIT)
            .count();

        assertThat(proceedCount).isEqualTo(activeCount);
        assertThat(waitCount).isEqualTo(totalUsers - activeCount);
    }
}