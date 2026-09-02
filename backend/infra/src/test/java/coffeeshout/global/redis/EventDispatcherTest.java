package coffeeshout.global.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;

import coffeeshout.fixture.BaseEventDummy;
import coffeeshout.global.metric.RedisStreamLatencyMetricService;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.CommonSettings;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private static final String STREAM_KEY = "room";

    private static final String WORK_QUEUE_STREAM = "settlement:result";

    @Mock
    RedisStreamLatencyMetricService latencyMetricService;

    MeterRegistry meterRegistry;
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
        meterRegistry = new SimpleMeterRegistry();
        final EventDispatcher dispatcher =
                new EventDispatcher(applicationContext, latencyMetricService, properties(), meterRegistry);
        dispatcher.initializeCounters();
        return dispatcher;
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
            dispatcher.handle(STREAM_KEY, event);

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
            dispatcher.handle(STREAM_KEY, event);

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
            assertThatCode(() -> dispatcher.handle(STREAM_KEY, event)).doesNotThrowAnyException();
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
            dispatcher.handle(STREAM_KEY, event);

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
            dispatcher.handle(STREAM_KEY, event);

            // then
            assertThat(consumer.received).isEmpty();
        }

        @Test
        @DisplayName("지연 메트릭 기록이 실패해도 Consumer는 이벤트를 받는다")
        void 지연_메트릭_기록이_실패해도_Consumer는_이벤트를_받는다() {
            // given
            willThrow(new RuntimeException("메트릭 실패"))
                    .given(latencyMetricService)
                    .recordLatency(anyString(), any(BaseEvent.class));
            final RecordingConsumer consumer = new RecordingConsumer();
            final EventDispatcher dispatcher = dispatcherWith(consumer);
            final BaseEventDummy event = BaseEventDummy.페이로드("메트릭");

            // when
            dispatcher.handle(STREAM_KEY, event);

            // then
            assertThat(consumer.received).containsExactly(event);
        }
    }

    @Nested
    @DisplayName("소비 카운터")
    class 소비_카운터 {

        // 컨슈머 정지 룰의 우변이다. 폴링이 살아 있다는 신호라 Consumer 처리 성공과 무관하게 센다.
        @Test
        void 소비하면_stream_태그가_붙은_카운터가_증가한다() {
            // given
            final EventDispatcher dispatcher = dispatcherWith(new RecordingConsumer());

            // when
            dispatcher.handle(STREAM_KEY, BaseEventDummy.페이로드("하나"));
            dispatcher.handle(STREAM_KEY, BaseEventDummy.페이로드("둘"));

            // then
            assertThat(consumedCounterOf(STREAM_KEY).count()).isEqualTo(2.0);
        }

        // 지연 히스토그램은 timestamp가 없는 이벤트를 건너뛴다. 소비 카운터가 그 구멍을 안 갖는 것이
        // 전용 카운터를 둔 이유다. 히스토그램 _count를 쓰면 이 경우 정지 룰이 오탐한다.
        @Test
        void timestamp가_없어_지연을_못_재도_소비는_센다() {
            // given
            final EventDispatcher dispatcher = dispatcherWith(new RecordingConsumer());

            // when
            dispatcher.handle(STREAM_KEY, new BaseEventDummy(UUID.randomUUID().toString(), null, "무시각"));

            // then
            assertThat(consumedCounterOf(STREAM_KEY).count()).isEqualTo(1.0);
        }

        // rate()가 시계열의 첫 샘플을 증가로 세지 않아, 첫 소비 때 시계열이 생기면 그 한 건이 빠진다.
        @Test
        void 소비_전에도_브로드캐스트_스트림_카운터가_0으로_등록된다() {
            // given
            dispatcherWith();

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(consumedCounterOf(STREAM_KEY)).isNotNull();
                softly.assertThat(consumedCounterOf(STREAM_KEY).count()).isZero();
            });
        }

        // 이 성질이 RedisStreamConsumptionStalled가 작업 큐 스트림에서 영구 발화하지 않는 근거다.
        // 시계열이 없으면 룰의 and 우변이 매칭되지 않는다.
        @Test
        void listener가_없는_작업_큐_스트림은_카운터를_만들지_않는다() {
            // given
            dispatcherWith();

            // when & then
            assertThat(consumedCounterOf(WORK_QUEUE_STREAM)).isNull();
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

    record OtherEventDummy(String eventId, Instant timestamp) implements BaseEvent {}

    private Counter consumedCounterOf(String streamKey) {
        return meterRegistry
                .find("redis.stream.consumed")
                .tag("stream", streamKey)
                .counter();
    }

    private RedisStreamProperties properties() {
        final Map<String, StreamConfig> keys = new LinkedHashMap<>();
        keys.put(STREAM_KEY, new StreamConfig("concurrent", null, null, null, null, null));
        keys.put(WORK_QUEUE_STREAM, new StreamConfig(null, null, 10000, null, null, false));
        return new RedisStreamProperties(
                new CommonSettings(100, 10, Duration.ofSeconds(2), Duration.ofSeconds(5)), Map.of(), keys);
    }
}
