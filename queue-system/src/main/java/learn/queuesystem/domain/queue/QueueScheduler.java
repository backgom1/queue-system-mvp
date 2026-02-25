package learn.queuesystem.domain.queue;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import learn.queuesystem.application.service.QueueServiceV1;
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

    private final QueueServiceV1 queueServiceV1;
    private final SseEmitterService sseEmitterService;
    private final MeterRegistry meterRegistry;
    private final WaitingQueueRepository waitingQueueRepository;

    private final AtomicLong waitingQueueSize = new AtomicLong(0);
    private final AtomicLong proceedingQueueSize = new AtomicLong(0);

    public QueueScheduler(QueueServiceV1 queueServiceV1, SseEmitterService sseEmitterService, MeterRegistry meterRegistry, WaitingQueueRepository waitingQueueRepository) {
        this.queueServiceV1 = queueServiceV1;
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

//    @Scheduled(fixedDelay = 1000)
    public void scheduleActivation() {
        Sample sample = Timer.start(meterRegistry);
        int allowCount = new Random().nextInt(10) + 40;
        try {
            // 1. 입장할 사람들을 뽑아서 입장 처리 (Admission)
            processAdmission(allowCount);

            // 2. 아직 남아있는 사람들에게 순번 알림 (Rank Notification)
            notifyWaitingOrder();

            updateMetrics();
        } catch (Exception e) {
            log.error("Queue activation failed", e);
        } finally {
            sample.stop(meterRegistry.timer("queue.scheduler.time"));
        }
    }

    private void processAdmission(int count) {
        String key = "queue:wait:concert-iu-2025";

        Set<ZSetOperations.TypedTuple<String>> targets = waitingQueueRepository.popMin(key, count);

        if (targets == null || targets.isEmpty()) return;

        for (ZSetOperations.TypedTuple<String> tuple : targets) {
            String userUuid = tuple.getValue();
            waitingQueueRepository.addActiveToken(userUuid, 300);

            Map<String, String> data = Map.of(
                    "accessToken", java.util.UUID.randomUUID().toString(),
                    "redirectUrl", "/api/v1/ticket/entry"
            );
            sseEmitterService.send(userUuid, "admission", data);
        }
    }

    private void notifyWaitingOrder() {
        String key = "queue:wait:concert-iu-2025";
        sseEmitterService.getSessions().forEach((userUuid, emitter) -> {
            Long rank = waitingQueueRepository.getRank(key, userUuid);
            if (rank != null) {
                sseEmitterService.send(userUuid, "waiting", rank + 1);
            }
        });
    }

    private void updateMetrics() {
        var stats = queueServiceV1.getStats();
        waitingQueueSize.set(stats.waiting());
        proceedingQueueSize.set(stats.proceeding());
    }
}