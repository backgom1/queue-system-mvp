package learn.queuesystem.presentation.api.queue;

import jakarta.validation.Valid;
import learn.queuesystem.domain.queue.Queue;
import learn.queuesystem.domain.queue.QueueService;
import learn.queuesystem.presentation.api.common.ApiResponse;
import learn.queuesystem.presentation.api.queue.dto.QueueRequest;
import learn.queuesystem.presentation.api.queue.dto.QueueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/token")
    public ResponseEntity<ApiResponse<QueueResponse>> issueToken(@RequestBody @Valid QueueRequest request) {
        Queue queue = queueService.issueToken(request.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(QueueResponse.from(queue)));
    }

    @GetMapping("/token")
    public ResponseEntity<ApiResponse<QueueResponse>> getTokenInfo(@RequestParam Long userId) {
        Queue queue = queueService.getTokenInfo(userId);
        return ResponseEntity.ok(ApiResponse.success(QueueResponse.from(queue)));
    }
}
