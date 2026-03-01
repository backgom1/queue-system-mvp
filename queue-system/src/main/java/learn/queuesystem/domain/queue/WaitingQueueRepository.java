package learn.queuesystem.domain.queue;

import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Set;

public interface WaitingQueueRepository {

    Long registerAndGetRank(String key, String memberId);

    void remove(String key, String member);

    Long getRank(String key, String member);

    Long queueSize(String key);

    Set<String> getTopMembers(String key, long count);

    /*
        활성화된 큐 정보를 가져옵니다.
    */
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
     * 유저의 ContentId를 조회합니다.
     */
    String getUserContent(String userUuid);

    // [기존 메서드들 유지]
    long getDoneCount();

    /*
        선택한 상위 대기열을 가져오는 메서드
     */
    Set<ZSetOperations.TypedTuple<String>> popMin(String key, int count);

    Long enterQueueAtomically(String waitKey, String tokenKey, String tokenValue, String contentId, String userUuid, long nowMillis, int tokenTtlSeconds);

    void issueEnterTicketsInPipeline(String contentId, Set<String> userIds, int ttlSeconds);

    List<String> findQueueStatusByToken(String tokenKey);

    long countActiveTicketsV2(String contentId, long nowMillis);

    long countActiveTokensV2(long nowMillis);
}
