package coffeeshout.global.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import coffeeshout.fixture.BaseEventDummy;
import coffeeshout.global.metric.RedisStreamLatencyMetricService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;

@ExtendWith(MockitoExtension.class)
class EventDispatcherTest {

    @Mock
    RedisStreamLatencyMetricService latencyMetricService;

    GenericApplicationContext applicationContext;

    @AfterEach
    void tearDown() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @SuppressWarnings("unchecked")
    private EventDispatcher dispatcherWith(Object... consumerBeans) {
        applicationContext = new GenericApplicationContext();
        for (int i = 0; i < consumerBeans.length; i++) {
            final Object bean = consumerBeans[i];
            applicationContext.registerBean("consumer" + i, (Class<Object>) bean.getClass(), () -> bean);
        }
        applicationContext.refresh();
        return new EventDispatcher(applicationContext, latencyMetricService);
    }

    @Nested
    @DisplayName("이벤트 디스패치(handle)")
    class Handle {

        @Test
        @DisplayName("등록된 Consumer가 하나면 그 Consumer로 이벤트를 전달한다 (기존 동작 회귀)")
        void 등록된_Consumer가_하나면_그_Consumer로_이벤트를_전달한다() {
            // given
            final RecordingConsumer consumer = new RecordingConsumer();
            final EventDispatcher dispatcher = dispatcherWith(consumer);
            final BaseEventDummy event = BaseEventDummy.페이로드("단일");

            // when
            dispatcher.handle(event);

            // then
            assertThat(consumer.received).containsExactly(event);
        }

        @Test
        @DisplayName("같은 이벤트 타입의 모든 Consumer에 팬아웃한다")
        void 같은_이벤트_타입의_모든_Consumer에_팬아웃한다() {
            // given
            final RecordingConsumer first = new RecordingConsumer();
            final RecordingConsumer second = new RecordingConsumer();
            final EventDispatcher dispatcher = dispatcherWith(first, second);
            final BaseEventDummy event = BaseEventDummy.페이로드("팬아웃");

            // when
            dispatcher.handle(event);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(first.received).containsExactly(event);
                softly.assertThat(second.received).containsExactly(event);
            });
        }

        @Test
        @DisplayName("등록된 Consumer가 없으면 예외 없이 건너뛴다")
        void 등록된_Consumer가_없으면_예외_없이_건너뛴다() {
            // given
            final EventDispatcher dispatcher = dispatcherWith();
            final BaseEventDummy event = BaseEventDummy.페이로드("미등록");

            // when & then
            assertThatCode(() -> dispatcher.handle(event)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("앞선 Consumer가 실패해도 다음 Consumer는 이벤트를 받는다")
        void 앞선_Consumer가_실패해도_다음_Consumer는_이벤트를_받는다() {
            // given
            final FailingConsumer failing = new FailingConsumer();
            final RecordingConsumer next = new RecordingConsumer();
            final EventDispatcher dispatcher = dispatcherWith(failing, next);
            final BaseEventDummy event = BaseEventDummy.페이로드("격리");

            // when
            dispatcher.handle(event);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(failing.invoked).isTrue();
                softly.assertThat(next.received).containsExactly(event);
            });
        }

        @Test
        @DisplayName("다른 이벤트 타입의 Consumer는 호출되지 않는다")
        void 다른_이벤트_타입의_Consumer는_호출되지_않는다() {
            // given
            final RecordingConsumer consumer = new RecordingConsumer();
            final EventDispatcher dispatcher = dispatcherWith(consumer);
            final OtherEventDummy event = new OtherEventDummy(UUID.randomUUID().toString(), Instant.now());

            // when
            dispatcher.handle(event);

            // then
            assertThat(consumer.received).isEmpty();
        }

        @Test
        @DisplayName("지연 메트릭 기록이 실패해도 Consumer는 이벤트를 받는다")
        void 지연_메트릭_기록이_실패해도_Consumer는_이벤트를_받는다() {
            // given
            willThrow(new RuntimeException("메트릭 실패"))
                    .given(latencyMetricService)
                    .recordLatency(any(BaseEvent.class));
            final RecordingConsumer consumer = new RecordingConsumer();
            final EventDispatcher dispatcher = dispatcherWith(consumer);
            final BaseEventDummy event = BaseEventDummy.페이로드("메트릭");

            // when
            dispatcher.handle(event);

            // then
            assertThat(consumer.received).containsExactly(event);
        }
    }

    @Order(1)
    static class FailingConsumer implements Consumer<BaseEventDummy> {

        boolean invoked = false;

        @Override
        public void accept(BaseEventDummy event) {
            invoked = true;
            throw new RuntimeException("Consumer 처리 실패");
        }
    }

    @Order(2)
    static class RecordingConsumer implements Consumer<BaseEventDummy> {

        final List<BaseEventDummy> received = new ArrayList<>();

        @Override
        public void accept(BaseEventDummy event) {
            received.add(event);
        }
    }

    record OtherEventDummy(String eventId, Instant timestamp) implements BaseEvent {
    }
}
