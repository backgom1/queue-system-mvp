package learn.queuesystem.domain.queue;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import learn.queuesystem.presentation.api.sse.SseEmitterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class QueueScheduler {

    private final QueueService queueService;
    private final SseEmitterService sseEmitterService;
    private final MeterRegistry meterRegistry;

    private final AtomicLong waitingQueueSize = new AtomicLong(0);
    private final AtomicLong proceedingQueueSize = new AtomicLong(0);

    public QueueScheduler(QueueService queueService, SseEmitterService sseEmitterService, MeterRegistry meterRegistry) {
        this.queueService = queueService;
        this.sseEmitterService = sseEmitterService;
        this.meterRegistry = meterRegistry;

        Gauge.builder("queue.waiting.size", waitingQueueSize, AtomicLong::get)
            .description("Current number of users waiting in queue")
            .register(meterRegistry);

        Gauge.builder("queue.proceeding.size", proceedingQueueSize, AtomicLong::get)
            .description("Current number of users currently proceeding")
            .register(meterRegistry);
    }

    /**
     * 1초마다 대기열을 확인하여 상위 N명을 진입시킴
     * 그리고 대기 중인 사용자에게 현재 순번을 알림
     */
    @Scheduled(fixedDelay = 1000)
    public void scheduleActivation() {
        Timer.Sample sample = Timer.start(meterRegistry);
        Random random = new Random();

        //TODO : 해당 로직을 추후에 예매 페이지의 접근 수를 체크하여 허용되는 카운트로 변환되도록 작업을 변경 고려
        int allowCount = random.nextInt(11) + 30;
        try {
            queueService.activateTokens(allowCount);
            queueService.completeExpiredTokens(); // 추가: 완료 처리 시뮬레이션
            
            // 메트릭 업데이트
            updateMetrics();

            notifyWaitingOrder();
        } catch (Exception e) {
            log.error("Queue activation failed", e);
        } finally {
            sample.stop(meterRegistry.timer("queue.scheduler.time"));
        }
    }

    private void updateMetrics() {
        var stats = queueService.getStats();
        waitingQueueSize.set(stats.waiting());
        proceedingQueueSize.set(stats.proceeding());
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
