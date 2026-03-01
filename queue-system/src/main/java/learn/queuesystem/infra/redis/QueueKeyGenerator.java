package learn.queuesystem.infra.redis;

import static learn.queuesystem.domain.queue.RedisPrefix.*;

public record QueueKeyGenerator() {


    public static String getWaitKey(String contentId) {
        return WAIT_QUEUE.getKey() + contentId;
    }

    public static String generateContentActiveContentId(String contentId) {
        return ACTIVE_CONTENTS.getKey() + contentId;
    }

    public static String generateActiveKeyContentId(String contentId) {
        return WAIT_QUEUE.getKey() + contentId;
    }

    public static String activeContentIdUserId(String contentId, String userId) {
        return ACTIVE_CONTENTS.getKey() + ":" + contentId + ":" + userId;
    }

    public static String tokenKey(String token) {
        return TOKEN_WAIT.getKey() + ":" + token;
    }

    public static String activeTicketZsetKey(String contentId) {
        return "queue:v2:active:" + contentId;
    }

}
