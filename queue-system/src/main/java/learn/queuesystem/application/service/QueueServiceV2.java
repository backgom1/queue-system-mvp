package learn.queuesystem.application.service;

import learn.queuesystem.application.dto.EnterStatusDto;
import learn.queuesystem.application.dto.NextPollTime;
import learn.queuesystem.application.dto.QueueStatusDto;
import learn.queuesystem.domain.queue.QueueStatus;
import learn.queuesystem.domain.queue.WaitingQueueRepository;
import learn.queuesystem.infra.redis.QueueKeyGenerator;
import learn.queuesystem.infra.util.EncryptQueueTokenProvider;
import learn.queuesystem.presentation.api.queue.dto.QueueStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static learn.queuesystem.infra.redis.QueueKeyGenerator.getWaitKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueServiceV2 {

    private final WaitingQueueRepository waitingQueueRepository;
    private final EncryptQueueTokenProvider tokenProvider;

    public EnterStatusDto enterQueue(String userUuid, String contentId) {

        String issueKey = tokenProvider.issue();
        String tokenKey = QueueKeyGenerator.tokenKey(issueKey);
        Long rank = waitingQueueRepository.enterQueueAtomically(
                getWaitKey(contentId),
                tokenKey,
                userUuid + "|" + contentId,
                contentId,
                userUuid,
                System.currentTimeMillis(),
                60
        );
        if (rank == null) {
            throw new IllegalStateException("Failed to register user to waiting queue");
        }
        long position = rank + 1;
        NextPollTime nextPoll = calculateNextPoll(position);

        return new EnterStatusDto(position, nextPoll.getNextTime(), issueKey, "http://localhost:8080/api/v2/queue/status");
    }


    public QueueStatusDto findQueuePosition(String token) {
        var result = waitingQueueRepository.findQueueStatusByToken(QueueKeyGenerator.tokenKey(token));
        String status = result.get(0);
        long position = Long.parseLong(result.get(1));

        if (QueueStatus.DONE.name().equals(status)) {
            return new QueueStatusDto(QueueStatus.DONE, 0, 0, token, "https://ticket.api.com/api/v1/ticket/entry");
        }
        if (QueueStatus.EXPIRED.name().equals(status)) {
            return new QueueStatusDto(QueueStatus.EXPIRED, 0, 0, token, null);
        }

        NextPollTime nextPollTime = calculateNextPoll(position);

        return new QueueStatusDto(QueueStatus.WAIT, position, nextPollTime.getNextTime(), token, "http://localhost:8080/api/v2/queue/status");
    }

    public QueueStatsDto getStats(String contentId) {
        long nowMillis = System.currentTimeMillis();
        long waiting = waitingQueueRepository.queueSize(getWaitKey(contentId));
        long activeTickets = waitingQueueRepository.countActiveTicketsV2(contentId, nowMillis);
        long activeTokens = waitingQueueRepository.countActiveTokensV2(nowMillis);
        return new QueueStatsDto(waiting, activeTickets, activeTokens);
    }

    private NextPollTime calculateNextPoll(long rank) {
        if (rank > 10000) {
            return NextPollTime.NEXT_POLL_TIME_LEVEL_4;
        }
        if (rank > 5000) {
            return NextPollTime.NEXT_POLL_TIME_LEVEL_3;
        }
        if (rank > 100) {
            return NextPollTime.NEXT_POLL_TIME_LEVEL_2;
        }
        return NextPollTime.NEXT_POLL_TIME_LEVEL_1;
    }

}
