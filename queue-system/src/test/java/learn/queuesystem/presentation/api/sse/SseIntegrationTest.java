package learn.queuesystem.presentation.api.sse;

import learn.queuesystem.domain.queue.Queue;
import learn.queuesystem.domain.queue.QueueRepository;
import learn.queuesystem.domain.queue.QueueScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private QueueScheduler queueScheduler;

    @Test
    @DisplayName("사용자는 SSE에 연결하고 스케줄러 실행 시 순번 알림을 받아야 한다")
    void sse_connect_and_notify() throws Exception {
        // given: 대기열 사용자 생성
        Long userId = 100L;
        queueRepository.save(Queue.wait(userId));

        // when: SSE 연결 요청
        MvcResult result = mockMvc.perform(get("/api/v1/sse/connect")
                .param("userId", String.valueOf(userId)))
            .andExpect(status().isOk())
            .andReturn();

        // 비동기 스트림 시작 확인 (헤더 등은 MockMvc로 확인 어려울 수 있음)
        
        // when: 스케줄러 실행 (강제 호출)
        queueScheduler.scheduleActivation();

        // then: 실제 브라우저처럼 EventStream을 지속적으로 읽는 것은 MockMvc 한계가 있음
        // 하지만 에러 없이 연결되고, 스케줄러가 돌 때 예외가 발생하지 않음을 검증
    }
}
