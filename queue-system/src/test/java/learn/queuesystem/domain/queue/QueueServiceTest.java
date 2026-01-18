package learn.queuesystem.domain.queue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
            queueRepository.save(Queue.wait((long) i));
            // 생성 시간 차이를 두기 위해 잠시 대기 (실제 DB 테스트에선 필요 없을 수 있으나 명시적으로)
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

        // 순서 검증: ID 1~5는 PROCEED, 6~10은 WAIT (순차적 생성 가정)
        // 실제로는 CreatedAt 기준이므로 ID 순서와 다를 수 있지만, 위에서 순차적으로 넣었으므로 일치해야 함
        Queue firstUser = queueRepository.findByUserId(1L).orElseThrow();
        Queue lastUser = queueRepository.findByUserId((long) totalUsers).orElseThrow();

        assertThat(firstUser.getStatus()).isEqualTo(QueueStatus.PROCEED);
        assertThat(lastUser.getStatus()).isEqualTo(QueueStatus.WAIT);
    }
}
