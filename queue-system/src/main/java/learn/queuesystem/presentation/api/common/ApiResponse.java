package learn.queuesystem.presentation.api.common;

/**
 * 공통 API 응답 포맷
 * 성공/실패 여부와 관계없이 일관된 JSON 구조를 반환합니다.
 *
 * @param success 성공 여부
 * @param code    응답 코드 (예: "SUCCESS", "ERR_001")
 * @param message 사람이 읽을 수 있는 메시지
 * @param data    실제 데이터 (실패 시 null 가능)
 * @param <T>     데이터 타입
 */
public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "Request successfully processed", data);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
