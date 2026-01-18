package learn.queuesystem.domain.queue;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueService {

    private final QueueRepository queueRepository;

    /**
     * 대기열 토큰 발급 (대기 상태로 진입)
     * 이미 대기중인 경우 기존 토큰 정보 반환
     */
    @Transactional
    public Queue issueToken(Long userId) {
        return queueRepository.findByUserId(userId)
            .orElseGet(() -> queueRepository.save(Queue.wait(userId)));
    }

    /**
     * 토큰 상태 조회
     * 토큰이 없으면 예외 발생
     */
    public Queue getTokenInfo(Long userId) {
        return queueRepository.findByUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("대기열 정보가 존재하지 않습니다. userId=" + userId));
    }

    /**
     * 현재 사용자의 대기 순번 조회
     * 내 앞에 WAIT 상태인 사람이 몇 명인지 카운트
     */
    public long calculateRank(Long userId) {
        Queue queue = getTokenInfo(userId);
        if (queue.getStatus() != QueueStatus.WAIT) {
            return 0; // 이미 진입했거나 만료됨
        }
        
        // 나보다 먼저 온(createAt < my.createdAt) WAIT 상태인 사람 수
        // Repository에 countByStatusAndCreatedAtBefore 메서드 필요
        return queueRepository.countByStatusAndCreatedAtBefore(QueueStatus.WAIT, queue.getCreatedAt());
    }

    /**
     * 대기열 진입 처리 (스케줄러 등에서 호출 예상)
     * 정책: N명씩 순차적으로 PROCEED 상태로 변경
     * DB Lock: 동시성 이슈를 고려해야 하지만, MVP 단계에서는 단순 조회 및 업데이트로 진행
     */
    @Transactional
    public void activateTokens(int count) {
        var pageable = org.springframework.data.domain.PageRequest.of(0, count);
        var waitingQueues = queueRepository.findByStatusOrderByCreatedAtAsc(QueueStatus.WAIT, pageable);

        waitingQueues.forEach(Queue::proceed);
    }
    
    /**
     * 토큰 만료 처리
     */
     @Transactional
     public void expireToken(Long userId) {
        Queue queue = getTokenInfo(userId);
        queue.complete(); // 비즈니스 로직에 따라 complete 또는 expire 메서드 호출
     }
}
