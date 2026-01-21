package learn.queuesystem.domain.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QueueRepository extends JpaRepository<Queue, Long> {
    Optional<Queue> findByUserUuid(String userUuid);
    @Query("SELECT DISTINCT q.contentId FROM Queue q WHERE q.status = :status")
    List<String> findDistinctContentIdsByStatus(@Param("status") QueueStatus status);

    /**
     * 입장 처리 (Bulk Update): 상태를 PROCEED로 변경하고 입장 시간을 기록합니다.
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE Queue q SET q.status = 'PROCEED', q.enteredAt = :enteredAt WHERE q.userUuid = :userUuid AND q.status = 'WAIT'")
    void activateUser(@Param("userUuid") String userUuid, @Param("enteredAt") java.time.LocalDateTime enteredAt);

    /**
     * 만료 처리 (Bulk Update): 상태를 DONE으로 변경합니다.
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE Queue q SET q.status = 'DONE' WHERE q.userUuid = :userUuid")
    void completeUser(@Param("userUuid") String userUuid);
}