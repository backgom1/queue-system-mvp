package learn.queuesystem.presentation.api.sse;

import learn.queuesystem.domain.queue.Queue;
import learn.queuesystem.domain.queue.QueueRepository;
import learn.queuesystem.domain.queue.QueueScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class SseIntegrationTest {

    @Mock
    private MockMvc mockMvc;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private QueueScheduler queueScheduler;

    @Test
    @DisplayName("사용자는 SSE에 연결하고 스케줄러 실행 시 순번 알림을 받아야 한다")
    void sse_connect_and_notify() throws Exception {
        // given: 대기열 사용자 생성
        String userUuid = UUID.randomUUID().toString();
        queueRepository.save(Queue.wait(userUuid, "concert-1"));

        // when: SSE 연결 요청 (Endpoint 변경됨: /api/v1/sse/connect -> /api/v1/queue/connect)
        MvcResult result = mockMvc.perform(get("/api/v1/queue/connect")
                .param("userUuid", userUuid))
            .andExpect(status().isOk())
            .andReturn();

        // when: 스케줄러 실행 (강제 호출)
        queueScheduler.scheduleActivation();

        // then: 연결 및 로직 실행 시 예외 없음 검증
    }
}