package learn.queuesystem.domain.queue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueTest {

    @Test
    @DisplayName("대기열 생성 시 초기 상태는 WAIT이어야 한다")
    void createQueue_waitStatus() {
        // given
        Long userId = 1L;

        // when
        Queue queue = Queue.wait(userId);

        // then
        assertThat(queue.getUserId()).isEqualTo(userId);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.WAIT);
    }

    @Test
    @DisplayName("WAIT 상태에서 PROCEED로 전환할 수 있다")
    void proceed_success() {
        // given
        Queue queue = Queue.wait(1L);

        // when
        queue.proceed();

        // then
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.PROCEED);
        assertThat(queue.getEnteredAt()).isNotNull();
    }

    @Test
    @DisplayName("WAIT가 아닌 상태에서 PROCEED로 전환하면 예외가 발생한다")
    void proceed_fail() {
        // given
        Queue queue = Queue.wait(1L);
        queue.proceed(); // PROCEED 상태

        // when & then
        assertThatThrownBy(queue::proceed)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("대기 상태인 경우에만 진입이 가능합니다");
    }
}
