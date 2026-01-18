package learn.queuesystem.presentation.api.queue.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QueueRequest(
    @NotNull(message = "User ID cannot be null")
    @Positive(message = "User ID must be positive")
    Long userId
) {
}
