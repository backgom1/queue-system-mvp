package learn.queuesystem.domain.queue;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class QueueService {

    private final QueueRepository queueRepository;
    private final MeterRegistry meterRegistry;

    public QueueService(QueueRepository queueRepository, MeterRegistry meterRegistry) {
        this.queueRepository = queueRepository;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 대기열 진입 요청
     * 정책:
     * 1. 이미 진입/대기 중인 경우 해당 정보 반환
     * 2. 대기열이 없고 여유가 있다면 바로 진입(PROCEED) 상태로 생성 (이번 MVP에선 일단 무조건 WAIT로 시작하고 스케줄러가 넘겨주는 방식으로 단순화 가능하지만,
     *    명세에 "바로 입장" 케이스가 있으므로, 현재 대기자가 0명인 경우 바로 PROCEED로 생성하는 로직 추가 가능)
     */
    @Transactional
    public Queue enterQueue(String userUuid, String contentId) {
        Counter.builder("queue.enter.request")
            .tag("contentId", contentId)
            .register(meterRegistry)
            .increment();

        return queueRepository.findByUserUuid(userUuid)
            .orElseGet(() -> {
                Queue newQueue = Queue.wait(userUuid, contentId);
                return queueRepository.save(newQueue);
            });
    }

    public Queue getQueueInfo(String userUuid) {
        return queueRepository.findByUserUuid(userUuid)
            .orElseThrow(() -> new EntityNotFoundException("대기열 정보가 존재하지 않습니다. uuid=" + userUuid));
    }

    public long calculateRank(String userUuid) {
        Queue queue = getQueueInfo(userUuid);
        if (queue.getStatus() != QueueStatus.WAIT) {
            return 0; 
        }
        return queueRepository.countByStatusAndCreatedAtBefore(QueueStatus.WAIT, queue.getCreatedAt());
    }

    @Transactional
    public void activateTokens(int count) {
        var pageable = org.springframework.data.domain.PageRequest.of(0, count);
        var waitingQueues = queueRepository.findByStatusOrderByCreatedAtAsc(QueueStatus.WAIT, pageable);
        waitingQueues.forEach(Queue::proceed);
    }

    @Transactional
    public void completeExpiredTokens() {
        // 진입 후 5초가 지나면 완료(DONE) 처리 시뮬레이션
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusSeconds(5);
        List<Queue> proceedingQueues = queueRepository.findByStatus(QueueStatus.PROCEED);
        proceedingQueues.stream()
            .filter(q -> q.getEnteredAt() != null && q.getEnteredAt().isBefore(threshold))
            .forEach(Queue::complete);
    }

    public record QueueStats(long waiting, long proceeding, long done) {}

    public QueueStats getStats() {
        return new QueueStats(
            queueRepository.countByStatus(QueueStatus.WAIT),
            queueRepository.countByStatus(QueueStatus.PROCEED),
            queueRepository.countByStatus(QueueStatus.DONE)
        );
    }
}