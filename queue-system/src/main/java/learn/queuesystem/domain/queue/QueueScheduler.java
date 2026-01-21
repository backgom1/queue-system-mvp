package learn.queuesystem.domain.queue;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import learn.queuesystem.presentation.api.sse.SseEmitterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@Profile("!test")
public class QueueScheduler {

    private final QueueService queueService;
    private final SseEmitterService sseEmitterService;
    private final MeterRegistry meterRegistry;
    private final WaitingQueueRepository waitingQueueRepository;

    private final AtomicLong waitingQueueSize = new AtomicLong(0);
    private final AtomicLong proceedingQueueSize = new AtomicLong(0);

    public QueueScheduler(QueueService queueService, SseEmitterService sseEmitterService, MeterRegistry meterRegistry, WaitingQueueRepository waitingQueueRepository) {
        this.queueService = queueService;
        this.sseEmitterService = sseEmitterService;
        this.meterRegistry = meterRegistry;
        this.waitingQueueRepository = waitingQueueRepository;

        Gauge.builder("queue.waiting.size", waitingQueueSize, AtomicLong::get)
                .description("Current number of users waiting in queue")
                .register(meterRegistry);

        Gauge.builder("queue.proceeding.size", proceedingQueueSize, AtomicLong::get)
                .description("Current number of users currently proceeding")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 1000)
    public void scheduleActivation() {
        Sample sample = Timer.start(meterRegistry);
        Random random = new Random();
        int allowCount = random.nextInt(10) + 40;
        try {
            activateTokens(allowCount);
            updateMetrics();
        } catch (Exception e) {
            log.error("Queue activation failed", e);
        } finally {
            sample.stop(meterRegistry.timer("queue.scheduler.time"));
        }
    }

    private void activateTokens(int count) {
        // 1. Redis에서 즉시 추출 및 삭제 (원자적 작업)
        Set<ZSetOperations.TypedTuple<String>> targets = waitingQueueRepository.popMin("queue:wait:concert-iu-2025", count);

        if (targets == null || targets.isEmpty()) return;

        for (ZSetOperations.TypedTuple<String> tuple : targets) {
            String userUuid = tuple.getValue();
            Long rank = waitingQueueRepository.getRank("queue:wait:concert-iu-2025", userUuid);

            waitingQueueRepository.addActiveToken(userUuid, 300);

            if (rank > 0) {
                sseEmitterService.send(userUuid, "waiting", rank);
            } else {

                Map<String, String> data = java.util.Map.of(
                        "accessToken", java.util.UUID.randomUUID().toString(),
                        "redirectUrl", "/api/v1/ticket/entry"
                );
                // 3. SSE 알림 발송 (이후 설명할 비동기 처리 권장)
                sseEmitterService.send(userUuid, "admission", data);
            }
        }
    }

    private void updateMetrics() {
        var stats = queueService.getStats();
        waitingQueueSize.set(stats.waiting());
        proceedingQueueSize.set(stats.proceeding());
    }
}