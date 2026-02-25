package learn.queuesystem.presentation.dto;

import learn.queuesystem.domain.queue.QueueStatus;

public record QueueStatusResponse(String status, long rank, int nextPollMs, String token, String redirectUrl) {
}
