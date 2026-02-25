package learn.queuesystem.domain.queue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedisPrefix {

    ACTIVE_CONTENTS("queue:contents:active"),
    ACTIVE_QUEUE("queue:active"),
    WAIT_QUEUE("queue:wait"),
    TOKEN_WAIT("token:wait"),
    ACTIVE("active");

    private final String key;

}
