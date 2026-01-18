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
        Queue queue = queueService.enterQueue(request.userUuid(), request.contentId());

        // Case 1: 바로 진입 (PROCEED)
        if (queue.getStatus() == QueueStatus.PROCEED) {
            return ResponseEntity.ok(
                EnterQueueResponse.granted(
                    UUID.randomUUID().toString(), // accessToken (임시)
                    "https://ticket.api.com/api/v1/ticket/entry"
                )
            );
        }

        // Case 2: 대기열 진입 (WAIT)
        long rank = queueService.calculateRank(queue.getUserUuid());
        return ResponseEntity.ok(
            EnterQueueResponse.wait(
                UUID.randomUUID().toString(), // queueToken (임시)
                rank,
                "http://localhost:8080/api/v1/queue/connect"
            )
        );
    }

    @GetMapping("/stats")
    public ResponseEntity<QueueService.QueueStats> getStats() {
        return ResponseEntity.ok(queueService.getStats());
    }

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam(name = "userUuid", required = false) String userUuid,
                              @RequestHeader(name = "Authorization", required = false) String token) {
        
        if (userUuid == null) {
            throw new IllegalArgumentException("userUuid or Token required");
        }
        
        return sseEmitterService.connect(userUuid);
    }
}
