package learn.queuesystem.application.dto;

import lombok.Getter;

@Getter
public enum NextPollTime {

    NEXT_POLL_TIME_LEVEL_1(1_000),
    NEXT_POLL_TIME_LEVEL_2(3_000),
    NEXT_POLL_TIME_LEVEL_3(10_000),
    NEXT_POLL_TIME_LEVEL_4(15_000);

    private final int nextTime;

    NextPollTime(int nextTime) {
        this.nextTime = nextTime;
    }
}
