package learn.queuesystem.domain.queue;

import learn.queuesystem.infra.redis.QueueKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;


/**
 * 해당 로직은 대기열 사용자 -> 활성 사용자로 만들어주는 로직입니다.
 */

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class QueueSchedulerV2 {

    private final WaitingQueueRepository waitingQueueRepository;

    /*
        1. 대기열 queue → ZRANGE로 N명 조회 → ZREM으로 삭제
        2. 입장 티켓 key => queue:active:{contentId} value => memberId (TTL 30초)
     */
    @Scheduled(fixedDelay = 1000)
    public void queueActivation() {

        int allowCount = 100;

        Set<String> activeContents = waitingQueueRepository.getActiveContents();

        for (String activeContent : activeContents) {
            String waitKey = QueueKeyGenerator.getWaitKey(activeContent);

            Set<ZSetOperations.TypedTuple<String>> queueWaitUsers = waitingQueueRepository.popMin(waitKey, allowCount);
            if (queueWaitUsers == null || queueWaitUsers.isEmpty()) {
                continue;
            }

            Set<String> userIds = new HashSet<>();
            for (ZSetOperations.TypedTuple<String> tuple : queueWaitUsers) {
                if (tuple != null && tuple.getValue() != null) {
                    userIds.add(tuple.getValue());
                }
            }
            waitingQueueRepository.issueEnterTicketsInPipeline(activeContent, userIds, 30);
        }

    }


}
