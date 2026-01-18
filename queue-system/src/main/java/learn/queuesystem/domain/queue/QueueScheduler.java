package learn.queuesystem.domain.queue;

import learn.queuesystem.presentation.api.sse.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final QueueService queueService;
    private final SseEmitterService sseEmitterService;

    /**
     * 1초마다 대기열을 확인하여 상위 N명을 진입시킴
     * 그리고 대기 중인 사용자에게 현재 순번을 알림
     */
    @Scheduled(fixedDelay = 1000)
    public void scheduleActivation() {
        int allowCount = 10; // 한 번에 입장시킬 인원
        try {
            queueService.activateTokens(allowCount);
            notifyWaitingOrder();
        } catch (Exception e) {
            log.error("Queue activation failed", e);
        }
    }

    private void notifyWaitingOrder() {
        // SSE에 연결된 사용자들 중, 아직 대기 상태인 사용자에게 순번 발송
        sseEmitterService.getEmitters().forEach((userId, emitter) -> {
            try {
                long rank = queueService.calculateRank(userId);
                if (rank > 0) {
                    sseEmitterService.send(userId, "QueueUpdate", rank);
                } else {
                    // 순번이 0이면 이미 진입했거나 다른 상태 -> 완료 알림 보내도 됨
                    sseEmitterService.send(userId, "QueueUpdate", "Entered");
                }
            } catch (Exception e) {
                // 특정 유저 오류는 무시하고 계속 진행
            }
        });
    }
}
