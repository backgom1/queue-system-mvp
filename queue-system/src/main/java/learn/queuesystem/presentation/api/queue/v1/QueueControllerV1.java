package learn.queuesystem.presentation.api.queue.v1;

import jakarta.validation.Valid;
import learn.queuesystem.application.service.QueueServiceV1;
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
public class QueueControllerV1 {

    private final QueueServiceV1 queueServiceV1;
    private final SseEmitterService sseEmitterService;

    @PostMapping("/enter")
    public ResponseEntity<EnterQueueResponse> enterQueue(@RequestBody @Valid EnterQueueRequest request) {
        Long queue = queueServiceV1.enterQueue(request.userUuid(), request.contentId());

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
    public ResponseEntity<QueueServiceV1.QueueStats> getStats() {
        return ResponseEntity.ok(queueServiceV1.getStats());
    }

    @GetMapping("/rank")
    public ResponseEntity<EnterQueueResponse> getRank(@RequestParam String userUuid, @RequestParam String contentId) {
        if (queueServiceV1.isAllowed(userUuid)) {
            return ResponseEntity.ok(
                    EnterQueueResponse.granted(
                            UUID.randomUUID().toString(),
                            "https://ticket.api.com/api/v1/ticket/entry"
                    )
            );
        }

        long rank = queueServiceV1.calculateRankWithContentId(contentId, userUuid);
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
