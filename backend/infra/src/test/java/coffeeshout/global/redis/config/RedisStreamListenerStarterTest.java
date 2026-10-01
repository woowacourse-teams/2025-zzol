package coffeeshout.global.redis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import coffeeshout.fixture.BaseEventDummy;
import coffeeshout.global.redis.EventDispatcher;
import coffeeshout.global.redis.stream.StreamRecordFields;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.util.ErrorHandler;

@ExtendWith(MockitoExtension.class)
class RedisStreamListenerStarterTest {

    private static final String STREAM_KEY = "room";

    @Mock
    private RedisStreamProperties properties;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private EventDispatcher eventDispatcher;

    @Mock
    private StreamTracePropagator streamTracePropagator;

    @Mock
    private ApplicationContext applicationContext;

    private ObjectMapper objectMapper;
    private RedisStreamContainerRegistry containerRegistry;
    private MeterRegistry meterRegistry;
    private RedisStreamListenerStarter starter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerSubtypes(BaseEventDummy.class);

        containerRegistry = new RedisStreamContainerRegistry();
        meterRegistry = new SimpleMeterRegistry();
        starter = new RedisStreamListenerStarter(
                properties,
                redisConnectionFactory,
                stringRedisTemplate,
                objectMapper,
                eventDispatcher,
                streamTracePropagator,
                applicationContext,
                containerRegistry,
                meterRegistry);
    }

    @Nested
    class 리스너_생성을_결정할_때 {

        @Test
        void listener_enabled가_false인_스트림은_브로드캐스트_컨테이너를_만들지_않는다() {
            // given: 컨슈머 그룹 전용 작업 큐 스트림(#1610)
            given(properties.keys())
                    .willReturn(Map.of(
                            "settlement:result",
                            new RedisStreamProperties.StreamConfig(null, null, 10000, null, null, false)));

            // when
            starter.streamContainers();

            // then: 컨테이너 생성 경로(스레드풀 조회·시작 오프셋 해석)에 진입하지 않는다
            verifyNoInteractions(applicationContext);
            verifyNoInteractions(stringRedisTemplate);
        }

        // 소비 카운터를 리스너와 같은 자리에서 만들기 때문에 성립하는 성질이다. 이 스트림에
        // 시계열이 생기면 RedisStreamConsumptionStalled가 "발행은 되는데 소비가 0"으로 읽어
        // 영구 발화한다(#1744).
        @Test
        void listener_enabled가_false인_스트림은_소비_카운터도_만들지_않는다() {
            // given
            given(properties.keys())
                    .willReturn(Map.of(
                            "settlement:result",
                            new RedisStreamProperties.StreamConfig(null, null, 10000, null, null, false)));

            // when
            starter.streamContainers();

            // then
            assertThat(소비_카운터("settlement:result")).isNull();
        }
    }

    @Nested
    class 구독_요청을_생성할_때 {

        @BeforeEach
        void stubEmptyStream() {
            스트림_마지막_레코드를_반환한다(List.of());
        }

        @Test
        void 어떤_예외가_발생해도_구독을_취소하지_않는다() {
            // given
            final StreamReadRequest<String> request = starter.buildReadRequest(STREAM_KEY);

            // when
            final Predicate<Throwable> cancelOnError = request.getCancelSubscriptionOnError();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cancelOnError.test(new RuntimeException("리스너 처리 실패")))
                        .isFalse();
                softly.assertThat(cancelOnError.test(new RedisSystemException("연결 오류", new IOException())))
                        .isFalse();
                softly.assertThat(cancelOnError.test(new Error("심각한 오류"))).isFalse();
            });
        }

        @Test
        void 컨텍스트_종료_중에는_구독_취소를_허용한다() {
            // given — 종료 중 취소를 막으면 파괴된 커넥션 팩토리에 폴링을 무한 재시도한다
            final StreamReadRequest<String> request = starter.buildReadRequest(STREAM_KEY);
            starter.onContextClosed(new ContextClosedEvent(applicationContext));

            // when & then
            assertThat(request.getCancelSubscriptionOnError().test(new RuntimeException("종료 중 오류")))
                    .isTrue();
        }

        @Test
        void 빈_파괴_시점에도_구독_취소를_허용한다() {
            // given — refresh 실패 시에는 ContextClosedEvent 없이 @PreDestroy만 호출된다 (ADR-0022)
            final StreamReadRequest<String> request = starter.buildReadRequest(STREAM_KEY);
            starter.markStopping();

            // when & then
            assertThat(request.getCancelSubscriptionOnError().test(new RuntimeException("파괴 후 폴링 오류")))
                    .isTrue();
        }

        @Test
        void 스트림_키와_오류_핸들러가_설정된다() {
            // when
            final StreamReadRequest<String> request = starter.buildReadRequest(STREAM_KEY);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(request.getStreamOffset().getKey()).isEqualTo(STREAM_KEY);
                softly.assertThat(request.getErrorHandler()).isNotNull();
            });
        }
    }

    @Nested
    class 시작_오프셋을_해석할_때 {

        @Test
        void 스트림에_레코드가_있으면_마지막_ID부터_시작한다() {
            // given — 기동 이전 메시지는 건너뛰고 이후 메시지만 소비한다 (ADR-0022)
            final MapRecord<String, Object, Object> lastRecord = StreamRecords.newRecord()
                    .in(STREAM_KEY)
                    .ofMap(Map.<Object, Object>of(StreamRecordFields.PAYLOAD, "마지막 메시지"))
                    .withId(RecordId.of("1718000000000-5"));
            스트림_마지막_레코드를_반환한다(List.of(lastRecord));

            // when
            final StreamReadRequest<String> request = starter.buildReadRequest(STREAM_KEY);

            // then
            assertThat(request.getStreamOffset().getOffset()).isEqualTo(ReadOffset.from("1718000000000-5"));
        }

        @Test
        void 스트림이_비어있으면_처음부터_시작한다() {
            // given
            스트림_마지막_레코드를_반환한다(List.of());

            // when
            final StreamReadRequest<String> request = starter.buildReadRequest(STREAM_KEY);

            // then
            assertThat(request.getStreamOffset().getOffset()).isEqualTo(ReadOffset.from("0-0"));
        }

        @Test
        void 스트림_조회_결과가_null이면_처음부터_시작한다() {
            // given
            스트림_마지막_레코드를_반환한다(null);

            // when
            final StreamReadRequest<String> request = starter.buildReadRequest(STREAM_KEY);

            // then
            assertThat(request.getStreamOffset().getOffset()).isEqualTo(ReadOffset.from("0-0"));
        }
    }

    @Nested
    class 구독_시작을_대기할_때 {

        @BeforeEach
        void stubCommonSettings() {
            given(properties.commonSettings())
                    .willReturn(new RedisStreamProperties.CommonSettings(
                            100, 10, Duration.ofSeconds(2), Duration.ofSeconds(5)));
        }

        @Test
        void 제한_시간_내에_시작되면_정상_반환한다() throws InterruptedException {
            // given
            final Subscription subscription = mock(Subscription.class);
            given(subscription.await(any())).willReturn(true);

            // when & then
            assertThatCode(() -> starter.awaitSubscriptionStart(STREAM_KEY, subscription))
                    .doesNotThrowAnyException();
        }

        @Test
        void 제한_시간_내에_시작되지_않으면_기동을_실패시킨다() throws InterruptedException {
            // given — Stream은 메시지 흐름의 필수 경로이므로 구독 없는 기동은 fail-fast (ADR-0022)
            final Subscription subscription = mock(Subscription.class);
            given(subscription.await(any())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> starter.awaitSubscriptionStart(STREAM_KEY, subscription))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(STREAM_KEY);
        }

        @Test
        void 대기_중_인터럽트되면_기동을_실패시키고_인터럽트_상태를_보존한다() throws InterruptedException {
            // given
            final Subscription subscription = mock(Subscription.class);
            given(subscription.await(any())).willThrow(new InterruptedException());

            // when & then — Thread.interrupted()는 검증과 동시에 플래그를 클리어해 다른 테스트 오염을 막는다
            assertThatThrownBy(() -> starter.awaitSubscriptionStart(STREAM_KEY, subscription))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(STREAM_KEY);
            assertThat(Thread.interrupted()).isTrue();
        }
    }

    @Nested
    class 스트림_오류를_처리할_때 {

        private Logger logger;
        private ListAppender<ILoggingEvent> appender;

        @BeforeEach
        void attachAppender() {
            스트림_마지막_레코드를_반환한다(List.of());
            logger = (Logger) LoggerFactory.getLogger(RedisStreamListenerStarter.class);
            appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
        }

        @AfterEach
        void detachAppender() {
            logger.detachAppender(appender);
        }

        @Test
        void 종료_중에는_어떤_오류도_ERROR로_기록하지_않는다() {
            // given
            final ErrorHandler errorHandler =
                    Objects.requireNonNull(starter.buildReadRequest(STREAM_KEY).getErrorHandler());
            starter.onContextClosed(new ContextClosedEvent(applicationContext));

            // when
            errorHandler.handleError(
                    new IllegalStateException("LettuceConnectionFactory was destroyed and cannot be used anymore"));
            errorHandler.handleError(new RuntimeException("종료 중 임의 오류"));

            // then — appender.list는 같은 클래스 Logger를 쓰는 다른 테스트의 잔여 백그라운드
            // 스레드가 동시에 append할 수 있어 순회 전 스냅샷으로 뜬다 (ConcurrentModificationException 방지)
            assertThat(List.copyOf(appender.list)).noneMatch(event -> event.getLevel() == Level.ERROR);
        }

        @Test
        void 평시_오류는_ERROR로_기록한다() {
            // given
            final ErrorHandler errorHandler =
                    Objects.requireNonNull(starter.buildReadRequest(STREAM_KEY).getErrorHandler());

            // when
            errorHandler.handleError(new RuntimeException("폴링 실패"));

            // then
            assertThat(List.copyOf(appender.list)).anyMatch(event -> event.getLevel() == Level.ERROR);
        }
    }

    @Nested
    class 메시지를_수신할_때 {

        @Test
        void 정상_메시지는_EventDispatcher에_위임한다() throws JsonProcessingException {
            // given
            stubPropagatorToRunTask();
            final BaseEventDummy event = BaseEventDummy.페이로드("정상 메시지");
            final MapRecord<String, String, String> message = 메시지(objectMapper.writeValueAsString(event));

            // when
            starter.onMessage(message);

            // then
            final ArgumentCaptor<BaseEventDummy> captor = ArgumentCaptor.forClass(BaseEventDummy.class);
            verify(eventDispatcher).handle(eq(STREAM_KEY), captor.capture());
            assertThat(captor.getValue().eventId()).isEqualTo(event.eventId());
        }

        @Test
        void 구형_raw_필드만_있는_레코드도_처리한다() throws JsonProcessingException {
            // given — 전환 이전에 발행된 ObjectRecord 포맷 메시지 폴백
            stubPropagatorToRunTask();
            final BaseEventDummy event = BaseEventDummy.페이로드("구형 메시지");
            final MapRecord<String, String, String> message = StreamRecords.newRecord()
                    .in(STREAM_KEY)
                    .ofMap(Map.of(StreamRecordFields.LEGACY_RAW, objectMapper.writeValueAsString(event)));

            // when
            starter.onMessage(message);

            // then
            final ArgumentCaptor<BaseEventDummy> captor = ArgumentCaptor.forClass(BaseEventDummy.class);
            verify(eventDispatcher).handle(eq(STREAM_KEY), captor.capture());
            assertThat(captor.getValue().eventId()).isEqualTo(event.eventId());
        }

        @Test
        void payload_필드가_없는_레코드는_무시한다() {
            // given
            final MapRecord<String, String, String> message =
                    StreamRecords.newRecord().in(STREAM_KEY).ofMap(Map.of("unknown", "값"));

            // when & then
            assertThatCode(() -> starter.onMessage(message)).doesNotThrowAnyException();
            verify(eventDispatcher, never()).handle(any(), any());
        }

        @Test
        void 역직렬화에_실패해도_예외를_전파하지_않는다() {
            // given
            final MapRecord<String, String, String> message = 메시지("json이 아닌 값");

            // when & then — 예외가 전파되면 Spring Data Redis가 구독을 취소할 수 있다
            assertThatCode(() -> starter.onMessage(message)).doesNotThrowAnyException();
            verify(eventDispatcher, never()).handle(any(), any());
        }

        @Test
        void 이벤트_처리_중_예외가_발생해도_전파하지_않는다() throws JsonProcessingException {
            // given
            stubPropagatorToRunTask();
            willThrow(new RuntimeException("처리 실패")).given(eventDispatcher).handle(any(), any());
            final MapRecord<String, String, String> message =
                    메시지(objectMapper.writeValueAsString(BaseEventDummy.페이로드("처리 실패 메시지")));

            // when & then — 예외가 전파되면 Spring Data Redis가 구독을 취소할 수 있다
            assertThatCode(() -> starter.onMessage(message)).doesNotThrowAnyException();
        }

        // 컨슈머 정지 룰의 우변이다. 폴링이 살아 있다는 신호라 수신 시점에 센다.
        @Test
        void 메시지를_받으면_소비_카운터가_증가한다() throws JsonProcessingException {
            // given
            stubPropagatorToRunTask();
            final MapRecord<String, String, String> message =
                    메시지(objectMapper.writeValueAsString(BaseEventDummy.페이로드("정상")));

            // when
            starter.onMessage(message);

            // then
            assertThat(소비_카운터(STREAM_KEY).count()).isEqualTo(1.0);
        }

        // 파싱에 실패한 메시지도 Redis에서 꺼내 온 것이라 소비다. 처리 성공 지점에서 세면 전 메시지가
        // 파싱에 실패하는 배포에서 정지 룰이 "폴링이 멎었다"고 오진한다.
        @Test
        void 역직렬화에_실패해도_소비_카운터는_증가한다() {
            // when
            starter.onMessage(메시지("json이 아닌 값"));
            starter.onMessage(StreamRecords.newRecord().in(STREAM_KEY).ofMap(Map.of("unknown", "값")));

            // then
            assertThat(소비_카운터(STREAM_KEY).count()).isEqualTo(2.0);
        }

        private void stubPropagatorToRunTask() {
            willAnswer(invocation -> {
                        invocation.getArgument(2, Runnable.class).run();
                        return null;
                    })
                    .given(streamTracePropagator)
                    .runInConsumerScope(any(), any(), any());
        }
    }

    private Counter 소비_카운터(String streamKey) {
        return meterRegistry
                .find("redis.stream.consumed")
                .tag("stream", streamKey)
                .counter();
    }

    private MapRecord<String, String, String> 메시지(String payload) {
        return StreamRecords.newRecord().in(STREAM_KEY).ofMap(Map.of(StreamRecordFields.PAYLOAD, payload));
    }

    private void 스트림_마지막_레코드를_반환한다(List<MapRecord<String, Object, Object>> records) {
        willReturn(streamOperations).given(stringRedisTemplate).opsForStream();
        willReturn(records).given(streamOperations).reverseRange(eq(STREAM_KEY), any(), any());
    }
}
