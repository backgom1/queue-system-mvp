package learn.queuesystem.domain.queue;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "queues")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Queue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userUuid;

    @Column(nullable = false)
    private String contentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status;

    private LocalDateTime enteredAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Queue(String userUuid, String contentId) {
        validateUserUuid(userUuid);
        this.userUuid = userUuid;
        this.contentId = contentId;
        this.status = QueueStatus.WAIT;
    }

    public static Queue wait(String userUuid, String contentId) {
        return new Queue(userUuid, contentId);
    }

    /**
     * 대기 -> 진입 가능 상태로 전환
     */
    public void proceed() {
        if (this.status != QueueStatus.WAIT) {
            throw new IllegalStateException("대기 상태인 경우에만 진입이 가능합니다.");
        }
        this.status = QueueStatus.PROCEED;
        this.enteredAt = LocalDateTime.now();
    }

    /**
     * 진입 -> 완료 상태로 전환
     */
    public void complete() {
        if (this.status != QueueStatus.PROCEED) {
            throw new IllegalStateException("진입 중인 경우에만 완료가 가능합니다.");
        }
        this.status = QueueStatus.DONE;
    }

    private void validateUserUuid(String userUuid) {
        if (userUuid == null || userUuid.isBlank()) {
            throw new IllegalArgumentException("유효하지 않은 사용자 UUID입니다.");
        }
    }
}
