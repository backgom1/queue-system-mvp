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
import org.springframework.transaction.annotation.Transactional;

import static learn.queuesystem.infra.redis.QueueKeyGenerator.getWaitKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueServiceV2 {

    private final WaitingQueueRepository waitingQueueRepository;
    private final EncryptQueueTokenProvider tokenProvider;

    @Transactional
    public EnterStatusDto enterQueue(String userUuid, String contentId) {

        if (Boolean.TRUE.equals(waitingQueueRepository.hasActiveToken(userUuid))) {
            waitingQueueRepository.remove(getWaitKey(contentId), userUuid);
        }

        String issueKey = tokenProvider.issue();
        String tokenKey = QueueKeyGenerator.tokenKey(issueKey);
        waitingQueueRepository.issueTokenKey(tokenKey, userUuid + "|" + contentId, 60);

        Long rank = waitingQueueRepository.registerAndGetRank(getWaitKey(contentId), userUuid);
        if (rank == null) {
            throw new IllegalStateException("Failed to register user to waiting queue");
        }
        long position = rank + 1;
        NextPollTime nextPoll = calculateNextPoll(position);

        waitingQueueRepository.activeContent(contentId);

        return new EnterStatusDto(position, nextPoll.getNextTime(), issueKey, "http://localhost:8080/api/v2/queue/status");
    }


    @Transactional(readOnly = true)
    public QueueStatusDto findQueuePosition(String token) {

        String value = waitingQueueRepository.getValue(QueueKeyGenerator.tokenKey(token));
        if (value == null || value.isBlank()) {
            return new QueueStatusDto(QueueStatus.EXPIRED, 0, 0, token, null);
        }

        String[] data = value.split("\\|", -1);
        if (data.length != 2 || data[0].isBlank() || data[1].isBlank()) {
            return new QueueStatusDto(QueueStatus.EXPIRED, 0, 0, token, null);
        }

        String userUuid = data[0];
        String contentId = data[1];

        Boolean activeUser = waitingQueueRepository.isActiveUser(QueueKeyGenerator.activeContentIdUserId(contentId, userUuid));

        if (activeUser) {
            return new QueueStatusDto(QueueStatus.DONE, 0, 0, token, "https://ticket.api.com/api/v1/ticket/entry");
        }

        Long rank = waitingQueueRepository.getRank(getWaitKey(contentId), userUuid);
        if (rank == null) {
            return new QueueStatusDto(QueueStatus.EXPIRED, 0, 0, token, null);
        }
        long position = rank + 1;
        NextPollTime nextPollTime = calculateNextPoll(position);

        return new QueueStatusDto(QueueStatus.WAIT, position, nextPollTime.getNextTime(), token, "http://localhost:8080/api/v2/queue/status");
    }

    @Transactional(readOnly = true)
    public QueueStatsDto getStats(String contentId) {
        long waiting = waitingQueueRepository.queueSize(getWaitKey(contentId));
        long activeTickets = waitingQueueRepository.countKeysByPattern("queue:contents:active:" + contentId + ":*");
        long activeTokens = waitingQueueRepository.countKeysByPattern("token:wait:*");
        return new QueueStatsDto(waiting, activeTickets, activeTokens);
    }

    private NextPollTime calculateNextPoll(long rank) {
        if (rank > 10000) {
            return NextPollTime.NEXT_POLL_TIME_HIGH;
        }
        if (rank > 5000) {
            return NextPollTime.NEXT_POLL_TIME_MEDIUM;
        }
        return NextPollTime.NEXT_POLL_TIME_LOW;
    }
}
