package learn.queuesystem.presentation.api.queue.dto;

public record QueueStatsDto(long waiting, long activeTickets, long activeTokens) {
}
