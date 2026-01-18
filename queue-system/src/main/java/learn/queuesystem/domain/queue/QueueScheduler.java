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
            queueService.completeExpiredTokens(); // 추가: 완료 처리 시뮬레이션
            notifyWaitingOrder();
        } catch (Exception e) {
            log.error("Queue activation failed", e);
        }
    }

    private void notifyWaitingOrder() {
        // SSE에 연결된 사용자들 중, 아직 대기 상태인 사용자에게 순번 발송
        sseEmitterService.getEmitters().forEach((userUuid, emitter) -> {
            try {
                long rank = queueService.calculateRank(userUuid);
                if (rank > 0) {
                    sseEmitterService.send(userUuid, "waiting", rank);
                } else {
                    // 순번이 0이면 입장 허가! 명세에 따른 데이터 전송
                    java.util.Map<String, String> data = java.util.Map.of(
                        "accessToken", java.util.UUID.randomUUID().toString(),
                        "redirectUrl", "/api/v1/ticket/entry" // 실제 티켓 예매 경로
                    );
                    sseEmitterService.send(userUuid, "admission", data); 
                }
            } catch (Exception e) {
                // 특정 유저 오류는 무시하고 계속 진행
            }
        });
    }
}
