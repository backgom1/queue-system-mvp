package learn.queuesystem.application.dto;

import learn.queuesystem.domain.queue.QueueStatus;

public record QueueStatusDto(QueueStatus status, long rank, int nextPollMs, String token, String redirectUrl) {
}
