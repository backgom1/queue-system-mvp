package learn.queuesystem.domain.queue;

import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

public interface WaitingQueueRepository {
    void register(String key, String member);

    void remove(String key, String member);

    Long getRank(String key, String member);

    Set<String> getTopMembers(String key, long count);

    void registerContent(String contentId);

    Set<String> getActiveContents();

    void removeContent(String contentId);

    void addActiveToken(String userUuid, long expirationTimestamp);

    Set<String> popExpiredActiveTokens(long currentTimestamp);

    long getQueueSize(String key);

    // [복구] PROCEEDING 조회를 위한 메서드
    long getActiveTokenSize();

    // [유지] DONE 조회를 위한 메서드
    void incrementDoneCount(String contentId);

    /**
     * 유저가 활성화된 토큰을 가지고 있는지 확인합니다.
     */
    Boolean hasActiveToken(String userUuid);

    /**
     * 유저의 접속 정보(ContentId)를 캐싱합니다. (중복 진입 방지용)
     */
    void saveUserContent(String userUuid, String contentId);

    /**
     * 유저가 이미 대기열/진행열에 존재하는지 확인합니다.
     */
    Boolean hasUser(String userUuid);

    /**
     * 유저의 ContentId를 조회합니다.
     */
    String getUserContent(String userUuid);

    // [기존 메서드들 유지]
    long getDoneCount();

    Set<ZSetOperations.TypedTuple<String>> popMin(String key, int count);
}