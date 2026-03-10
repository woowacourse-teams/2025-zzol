package coffeeshout.global.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

    @Test
    void 생성_시_PENDING_상태이고_retryCount는_0이다() {
        // when
        OutboxEvent event = OutboxEvent.create("room", "{\"type\":\"test\"}");

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getStreamKey()).isEqualTo("room");
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    void 상태_전이_PENDING에서_IN_PROGRESS를_거쳐_PUBLISHED로_변경된다() {
        // given
        OutboxEvent event = OutboxEvent.create("room", "{}");

        // when & then
        event.markInProgress();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.IN_PROGRESS);

        event.markPublished();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    void 발행_실패_시_PENDING으로_복귀하고_retryCount가_증가한다() {
        // given
        OutboxEvent event = OutboxEvent.create("room", "{}");
        event.markInProgress();

        // when
        event.incrementRetryCount();
        event.setStatusPending();

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
    }

    @Test
    void 재시도_10회_실패_시_DEAD_LETTER로_전환된다() {
        // given
        OutboxEvent event = OutboxEvent.create("room", "{}");

        // when
        IntStream.range(0, 10).forEach(i -> event.incrementRetryCount());
        event.markDeadLetter();

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTER);
        assertThat(event.getRetryCount()).isEqualTo(10);
    }
}
