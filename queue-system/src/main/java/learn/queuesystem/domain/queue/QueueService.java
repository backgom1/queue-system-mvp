package learn.queuesystem.domain.queue;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
public class QueueService {

    private final WaitingQueueRepository waitingQueueRepository;
    private final MeterRegistry meterRegistry;

    private static final String WAIT_KEY_PREFIX = "queue:wait:";

    public QueueService(
            WaitingQueueRepository waitingQueueRepository,
            MeterRegistry meterRegistry) {
        this.waitingQueueRepository = waitingQueueRepository;
        this.meterRegistry = meterRegistry;
    }

    private String getWaitKey(String contentId) {
        return WAIT_KEY_PREFIX + contentId;
    }

    public Long enterQueue(String userUuid, String contentId) {
        Counter.builder("queue.enter.request")
                .tag("contentId", contentId)
                .register(meterRegistry)
                .increment();

        if (Boolean.TRUE.equals(waitingQueueRepository.hasActiveToken(userUuid))) {
            waitingQueueRepository.remove(getWaitKey(contentId), userUuid);
        }

        waitingQueueRepository.register(getWaitKey(contentId), userUuid);


        return waitingQueueRepository.getRank(getWaitKey(contentId), userUuid);
    }

    public long calculateRank(String userUuid) {
        String contentId = waitingQueueRepository.getUserContent(userUuid);
        if (contentId == null) {
            return 0;
        }
        return calculateRankWithContentId(contentId, userUuid);
    }

    public long calculateRankWithContentId(String contentId, String userUuid) {
        String key = getWaitKey(contentId);
        Long rank = waitingQueueRepository.getRank(key, userUuid);
        return (rank == null) ? 0 : rank + 1;
    }

    public void activateTokens(int count) {
        //현재 count만큼의 앞순서 불러오기
        Set<String> activeContentIds = waitingQueueRepository.getActiveContents();

        for (String contentId : activeContentIds) {
            activateTokensForContent(contentId, count);
        }
    }

    public void activateTokensForContent(String contentId, int count) {
        String key = getWaitKey(contentId);
        Set<String> targets = waitingQueueRepository.getTopMembers(key, count);

        if (targets == null || targets.isEmpty()) {
            waitingQueueRepository.removeContent(contentId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (String userUuid : targets) {

            waitingQueueRepository.remove(key, userUuid);
            long expireAt = System.currentTimeMillis() + 5000;
            waitingQueueRepository.addActiveToken(userUuid, expireAt);
        }

        log.info("Activated {} users for contentId: {}", targets.size(), contentId);
    }

    @Transactional
    public void completeExpiredTokens() {
        long now = System.currentTimeMillis();
        Set<String> expiredUsers = waitingQueueRepository.popExpiredActiveTokens(now);

        if (expiredUsers.isEmpty()) {
            return;
        }

        for (String userUuid : expiredUsers) {
            String contentId = waitingQueueRepository.getUserContent(userUuid);
            if (contentId != null) {
                waitingQueueRepository.incrementDoneCount(contentId);
            }
        }

        log.info("Completed {} expired tokens", expiredUsers.size());
    }

    public record QueueStats(long waiting, long proceeding, long done) {
    }

    public boolean isAllowed(String userUuid) {
        return Boolean.TRUE.equals(waitingQueueRepository.hasActiveToken(userUuid));
    }

    public QueueStats getStats() {
        long waiting = 0;
        Set<String> activeContents = waitingQueueRepository.getActiveContents();
        for (String contentId : activeContents) {
            waiting += waitingQueueRepository.getQueueSize(getWaitKey(contentId));
        }

        long proceeding = waitingQueueRepository.getActiveTokenSize();
        long done = waitingQueueRepository.getDoneCount();

        return new QueueStats(waiting, proceeding, done);
    }
}
