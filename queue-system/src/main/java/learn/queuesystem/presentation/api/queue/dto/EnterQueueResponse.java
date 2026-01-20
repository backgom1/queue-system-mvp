package learn.queuesystem.presentation.api.queue.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EnterQueueResponse(
    String resultCode,
    String message,
    EnterQueueData data
) {
    public record EnterQueueData(
        Long currentRank,
        String queueToken,    // 대기 중일 때 사용
        String accessToken,   // 입장 완료 시 사용
        String redirectUrl,
        String sseUrl
    ) {}

    // Case 1: 바로 입장
    public static EnterQueueResponse granted(String accessToken, String redirectUrl) {
        return new EnterQueueResponse(
            "ENTRY_GRANTED",
            "입장 가능!",
            new EnterQueueData(0L, null, accessToken, redirectUrl, null)
        );
    }

    // Case 2: 대기열 진입
    public static EnterQueueResponse wait(String queueToken, Long rank, String sseUrl) {
        return new EnterQueueResponse(
            "QUEUE_WAIT",
            "대기열 등록이 필요합니다.",
            new EnterQueueData(rank, queueToken, null, null, sseUrl)
        );
    }
}
