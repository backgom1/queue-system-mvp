package learn.queuesystem.domain.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface QueueRepository extends JpaRepository<Queue, Long> {
    Optional<Queue> findByUserUuid(String userUuid);

    /**
     * 특정 상태의 대기열을 생성일 순(먼저 온 순)으로 조회 (Limit 적용)
     */
    List<Queue> findByStatusOrderByCreatedAtAsc(QueueStatus status, Pageable pageable);

    long countByStatusAndCreatedAtBefore(QueueStatus status, java.time.LocalDateTime createdAt);

    long countByStatus(QueueStatus status);

    List<Queue> findByStatus(QueueStatus status);
}
