package learn.queuesystem.presentation.api.queue.v2;

import jakarta.validation.Valid;
import learn.queuesystem.application.dto.EnterStatusDto;
import learn.queuesystem.application.dto.QueueStatusDto;
import learn.queuesystem.application.service.QueueServiceV2;
import learn.queuesystem.presentation.api.common.ApiResponse;
import learn.queuesystem.presentation.api.queue.dto.EnterQueueRequest;
import learn.queuesystem.presentation.api.queue.dto.EnterQueueResponseV2;
import learn.queuesystem.presentation.api.queue.dto.QueueStatsDto;
import learn.queuesystem.presentation.dto.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/queue")
@RequiredArgsConstructor
public class QueueControllerV2 {

    private final QueueServiceV2 queueServiceV2;

    @PostMapping("/enter")
    public ResponseEntity<ApiResponse<EnterQueueResponseV2>> enterQueue(@RequestBody @Valid EnterQueueRequest request) {
        EnterStatusDto statusDto = queueServiceV2.enterQueue(request.userUuid(), request.contentId());
        return ResponseEntity.ok(ApiResponse.success(new EnterQueueResponseV2(
                        statusDto.rank(),
                        statusDto.nextPollMs(),
                        statusDto.token(),
                        statusDto.redirectUrl())
                )
        );
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<QueueStatusResponse>> checkQueueStatus(@RequestParam String token) {
        QueueStatusDto queuePosition = queueServiceV2.findQueuePosition(token);
        return ResponseEntity.ok(ApiResponse.success(
                        new QueueStatusResponse(
                                  queuePosition.status().toString()
                                , queuePosition.rank()
                                , queuePosition.nextPollMs()
                                , queuePosition.token()
                                , queuePosition.redirectUrl())
                )
        );
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<QueueStatsDto>> getStats(@RequestParam String contentId) {
        return ResponseEntity.ok(ApiResponse.success(queueServiceV2.getStats(contentId)));
    }

}
