package learn.queuesystem.presentation.api.queue.dto;

import jakarta.validation.constraints.NotBlank;

public record EnterQueueRequest(
    @NotBlank(message = "User UUID cannot be blank")
    String userUuid,
    
    @NotBlank(message = "Content ID cannot be blank")
    String contentId,
    
    String deviceId
) {
}
