package learn.queuesystem.application.dto;

import lombok.Getter;

@Getter
public enum NextPollTime {

    NEXT_POLL_TIME_HIGH(1),
    NEXT_POLL_TIME_MEDIUM(3),
    NEXT_POLL_TIME_LOW(10);

    private final int nextTime;

    NextPollTime(int nextTime) {
        this.nextTime = nextTime;
    }
}
