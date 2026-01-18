package learn.queuesystem.domain.queue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QueueStatus {
    WAIT("대기 중"),
    PROCEED("진입 중"),
    DONE("완료"),
    EXPIRED("만료");

    private final String description;
}
