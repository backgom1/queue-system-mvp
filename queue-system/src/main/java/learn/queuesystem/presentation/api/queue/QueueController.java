package learn.queuesystem.presentation.api.queue;

import jakarta.validation.Valid;
import learn.queuesystem.domain.queue.Queue;
import learn.queuesystem.domain.queue.QueueService;
import learn.queuesystem.domain.queue.QueueStatus;
import learn.queuesystem.presentation.api.queue.dto.EnterQueueRequest;
import learn.queuesystem.presentation.api.queue.dto.EnterQueueResponse;
import learn.queuesystem.presentation.api.sse.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final SseEmitterService sseEmitterService;

    @PostMapping("/enter")
    public ResponseEntity<EnterQueueResponse> enterQueue(@RequestBody @Valid EnterQueueRequest request) {
        Long queue = queueService.enterQueue(request.userUuid(), request.contentId());

        if (queue <= 50) {
            return ResponseEntity.ok(
                    EnterQueueResponse.granted(
                            UUID.randomUUID().toString(), // accessToken (임시)
                            "https://ticket.api.com/api/v1/ticket/entry"
                    )
            );
        }

        return ResponseEntity.ok(
                EnterQueueResponse.wait(
                        UUID.randomUUID().toString(),
                        queue,
                        "http://localhost:8080/api/v1/queue/connect"
                )
        );
    }

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam(name = "userUuid", required = false) String userUuid,
                              @RequestHeader(name = "Authorization", required = false) String token) {

        if (userUuid == null) {
            throw new IllegalArgumentException("userUuid or Token required");
        }

        return sseEmitterService.connect(userUuid, "concert-iu-2025");
    }

    @GetMapping("/stats")
    public ResponseEntity<QueueService.QueueStats> getStats() {
        return ResponseEntity.ok(queueService.getStats());
    }

    @GetMapping("/rank")
    public ResponseEntity<EnterQueueResponse> getRank(@RequestParam String userUuid, @RequestParam String contentId) {
        if (queueService.isAllowed(userUuid)) {
            return ResponseEntity.ok(
                    EnterQueueResponse.granted(
                            UUID.randomUUID().toString(),
                            "https://ticket.api.com/api/v1/ticket/entry"
                    )
            );
        }

        long rank = queueService.calculateRankWithContentId(contentId, userUuid);
        // 대기열에도 없고 활성화도 안 되었다면? (예: 만료됨 or 미진입) -> rank 0 처리 하거나 에러.
        // 여기서는 rank 0이면 일단 wait로 0번을 주거나.. 로직 정의 필요.
        // 만약 rank == 0 인데 isAllowed가 false라면, 대기열에서 튕겨나간 것임.
        // 다시 enter를 유도해야 함.

        // rank == 0 인 경우: 대기열에 없음.
        // isAllowed도 false이므로, 이 유저는 줄을 서지 않았거나 이미 만료된 유저임.
        // 절대 입장시키면 안 됨. 다시 줄을 서도록 WAIT 상태로 응답하거나 에러 처리.
        return ResponseEntity.ok(
                EnterQueueResponse.wait(
                        null,
                        rank,
                        "http://localhost:8080/api/v1/queue/enter" // 다시 enter 하도록 유도
                )
        );
    }
}
