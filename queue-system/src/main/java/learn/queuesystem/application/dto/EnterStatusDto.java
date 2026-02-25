package learn.queuesystem.application.dto;

public record EnterStatusDto(long rank, int nextPollMs, String token, String redirectUrl) {
}
