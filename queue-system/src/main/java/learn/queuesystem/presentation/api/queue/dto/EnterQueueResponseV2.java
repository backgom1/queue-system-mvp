package learn.queuesystem.presentation.api.queue.dto;

public record EnterQueueResponseV2(long rank, int nextPollMs, String token, String redirectUrl) {
}
