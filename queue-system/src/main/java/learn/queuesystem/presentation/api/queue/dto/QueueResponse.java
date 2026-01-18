package learn.queuesystem.presentation.api.queue.dto;

import learn.queuesystem.domain.queue.Queue;
import learn.queuesystem.domain.queue.QueueStatus;

import java.time.LocalDateTime;

public record QueueResponse(
    Long userId,
    QueueStatus status,
    LocalDateTime enteredAt,
    LocalDateTime createdAt
) {
    public static QueueResponse from(Queue queue) {
        return new QueueResponse(
            queue.getUserId(),
            queue.getStatus(),
            queue.getEnteredAt(),
            queue.getCreatedAt()
        );
    }
}
