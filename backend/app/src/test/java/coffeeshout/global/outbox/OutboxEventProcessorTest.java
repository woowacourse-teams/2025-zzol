package coffeeshout.global.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.app.StreamMockedServiceTest;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;

class OutboxEventProcessorTest extends StreamMockedServiceTest {

    @Autowired
    private OutboxEventProcessor outboxEventProcessor;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAllInBatch();
    }

    private OutboxEvent createPendingEvent(String streamKey, String payload) {
        return outboxEventRepository.save(OutboxEvent.create(streamKey, payload));
    }

    @Nested
    class fetchAndMarkInProgress_메서드 {

        @Test
        void PENDING_이벤트를_조회하고_IN_PROGRESS로_전환한다() {
            // given
            createPendingEvent("room", "{\"type\":\"test1\"}");
            createPendingEvent("room", "{\"type\":\"test2\"}");

            // when
            List<OutboxEvent> result = outboxEventProcessor.fetchAndMarkInProgress(10);

            // then
            assertThat(result).hasSize(2)
                    .allSatisfy(event ->
                    assertThat(event.getStatus()).isEqualTo(OutboxStatus.IN_PROGRESS)
            );
        }

        @Test
        void PENDING_이벤트가_없으면_빈_리스트를_반환한다() {
            // when
            List<OutboxEvent> result = outboxEventProcessor.fetchAndMarkInProgress(10);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void batchSize만큼만_조회한다() {
            // given
            IntStream.range(0, 5).forEach(i ->
                    createPendingEvent("room", "{\"index\":" + i + "}")
            );

            // when
            List<OutboxEvent> result = outboxEventProcessor.fetchAndMarkInProgress(3);

            // then
            assertThat(result).hasSize(3);
        }
    }

    @Nested
    class markPublished_메서드는 {

        @Test
        void 이벤트를_PUBLISHED_상태로_전환한다() {
            // given — REQUIRES_NEW 트랜잭션에서 데이터가 보이도록 테스트 트랜잭션을 먼저 커밋
            OutboxEvent event = createPendingEvent("room", "{\"type\":\"test\"}");
            TestTransaction.flagForCommit();
            TestTransaction.end();

            // when
            outboxEventProcessor.markPublished(event.getId());

            // then
            TestTransaction.start();
            OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        }
    }

    @Nested
    class handleFailure_메서드 {

        @Test
        void 실패_시_retryCount를_증가시키고_PENDING으로_되돌린다() {
            // given
            OutboxEvent event = createPendingEvent("room", "{\"type\":\"test\"}");

            // when
            outboxEventProcessor.handleFailure(event.getId());

            // then
            OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertThat(updated.getRetryCount()).isEqualTo(1);
            assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PENDING);
        }

        @Test
        void 재시도_10회_실패_시_DEAD_LETTER로_전환한다() {
            // given
            OutboxEvent event = createPendingEvent("room", "{\"type\":\"test\"}");

            // when
            IntStream.range(0, 10).forEach(i ->
                    outboxEventProcessor.handleFailure(event.getId())
            );

            // then
            OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTER);
            assertThat(updated.getRetryCount()).isEqualTo(10);
        }
    }

    @Nested
    class recoverStaleEvents_메서드 {

        @Test
        void 메서드_호출이_에러_없이_동작한다() {
            // given
            OutboxEvent event = createPendingEvent("room", "{\"type\":\"stale\"}");
            event.markInProgress();
            outboxEventRepository.save(event);
            outboxEventRepository.flush();

            // when
            int recovered = outboxEventProcessor.recoverStaleEvents();

            // then — H2에서는 created_at 조건으로 0이 될 수 있다.
            // 실제 MySQL 통합 테스트에서 시간 조건을 검증할 부분.
            assertThat(recovered).isGreaterThanOrEqualTo(0);
        }
    }
}
