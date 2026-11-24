package coffeeshout.global.redis.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import coffeeshout.fixture.BaseEventDummy;
import coffeeshout.global.metric.RedisStreamPublishMetricService;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.CommonSettings;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Mono;

class StreamPublisherTest {

    private static final StreamKey KEY = () -> "room";

    @SuppressWarnings("unchecked")
    private final ReactiveStreamOperations<String, Object, Object> reactiveStreamOperations =
            mock(ReactiveStreamOperations.class);

    private final ReactiveStringRedisTemplate reactiveTemplate = mock(ReactiveStringRedisTemplate.class);
    private final RedisStreamProperties redisStreamProperties = new RedisStreamProperties(
            new CommonSettings(100, 10, Duration.ofSeconds(2), Duration.ofSeconds(5)),
            Map.of(),
            Map.of("room", new StreamConfig("concurrent", null, null, null, null, null)));

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final RedisStreamPublishMetricService metricService =
            new RedisStreamPublishMetricService(meterRegistry, redisStreamProperties);

    private StreamPublisher publisher() {
        doReturn(reactiveStreamOperations).when(reactiveTemplate).opsForStream();
        return new StreamPublisher(
                mock(StringRedisTemplate.class),
                reactiveTemplate,
                redisStreamProperties,
                new ObjectMapper().findAndRegisterModules(),
                mock(StreamTracePropagator.class),
                metricService,
                ContextSnapshotFactory.builder().build());
    }

    private StreamPublisher publisherWithFailureNotificationExecutor(Executor failureNotificationExecutor) {
        doReturn(reactiveStreamOperations).when(reactiveTemplate).opsForStream();
        return new StreamPublisher(
                mock(StringRedisTemplate.class),
                reactiveTemplate,
                redisStreamProperties,
                new ObjectMapper().findAndRegisterModules(),
                mock(StreamTracePropagator.class),
                metricService,
                ContextSnapshotFactory.builder().build(),
                failureNotificationExecutor);
    }

    private static Executor 작업을_거절하는_실행기() {
        return task -> {
            throw new RejectedExecutionException("실패 알림 스케줄러가 가득 참");
        };
    }

    @SafeVarargs
    private void givenXAddResults(Mono<RecordId> first, Mono<RecordId>... rest) {
        given(reactiveStreamOperations.add(any(MapRecord.class), any(XAddOptions.class)))
                .willReturn(first, rest);
    }

    private Timer roundTripTimer(String outcome) {
        return meterRegistry.timer(
                RedisStreamPublishMetricService.ROUND_TRIP_METRIC, "stream", "room", "outcome", outcome);
    }

    private BaseEventDummy 이벤트() {
        return BaseEventDummy.페이로드("비동기 발행");
    }

    @Nested
    class 비동기_발행 {

        @Test
        void XADD_응답을_기다리지_않고_바로_돌아온다() {
            // given — 응답이 끝내 오지 않는 XADD
            final StreamPublisher publisher = publisher();
            givenXAddResults(Mono.never());

            // when
            final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());

            // then — 호출 스레드는 응답을 기다리지 않고 돌아오고, 결과는 아직 정해지지 않았다
            assertThat(published).isNotDone();
        }

        @Test
        void XADD가_끝나면_돌려준_future가_완료된다() {
            // given
            final StreamPublisher publisher = publisher();
            givenXAddResults(Mono.just(RecordId.of("1-0")));

            // when
            final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());

            // then
            assertThat(published).succeedsWithin(Duration.ofSeconds(1));
        }

        @Test
        void 발행이_실패하면_future가_XADD_예외_그대로_실패한다() {
            // given
            final StreamPublisher publisher = publisher();
            final IllegalStateException failure = new IllegalStateException("Redis 연결 끊김");
            givenXAddResults(Mono.error(failure));

            // when
            final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());

            // then — Spring이 예외 타입으로 @MessageExceptionHandler를 고르므로 CompletionException으로 감싸지 않은
            // 원래 예외를 받는다
            assertThat(published.handle((ignored, error) -> error))
                    .succeedsWithin(Duration.ofSeconds(1))
                    .isSameAs(failure);
        }

        @Test
        void XADD_명령을_만들다_바로_실패해도_호출_스레드로_던지지_않고_future로_알린다() {
            // given — Mono를 돌려주기 전에 바로 던지는 XADD
            final StreamPublisher publisher = publisher();
            final IllegalStateException failure = new IllegalStateException("커넥션 없음");
            given(reactiveStreamOperations.add(any(MapRecord.class), any(XAddOptions.class)))
                    .willThrow(failure);

            // when
            final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());

            // then
            assertThat(published.handle((ignored, error) -> error))
                    .succeedsWithin(Duration.ofSeconds(1))
                    .isSameAs(failure);
        }

        @Test
        void 실패_알림을_넘기지_못해도_사라지지_않고_지금_스레드에서_알린다() {
            // given — 실패 알림을 넘길 곳이 작업을 거절하고, XADD는 실패한다
            final StreamPublisher publisher = publisherWithFailureNotificationExecutor(작업을_거절하는_실행기());
            final IllegalStateException failure = new IllegalStateException("Redis 연결 끊김");
            givenXAddResults(Mono.error(failure));

            // when
            final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());

            // then — 넘기지 못했다고 future가 끝나지 않은 채 남지 않고, XADD 예외 그대로 실패한다
            assertThat(published.handle((ignored, error) -> error))
                    .succeedsWithin(Duration.ofSeconds(1))
                    .isSameAs(failure);
        }
    }

    @Nested
    class 지표 {

        @Test
        void 호출_스레드가_기다리지_않아도_XADD_왕복_시간을_잰다() {
            // given — 100ms 뒤에 응답하는 XADD
            final StreamPublisher publisher = publisher();
            givenXAddResults(Mono.just(RecordId.of("1-0")).delayElement(Duration.ofMillis(100)));

            // when — 응답이 올 때까지 기다린다
            publisher.publishAsync(KEY, 이벤트()).join();

            // then — 응답이 온 때 왕복 시간이 기록된다
            final Timer roundTrip = roundTripTimer("success");
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(roundTrip.count()).isEqualTo(1);
                softly.assertThat(roundTrip.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(100);
            });
        }

        @Test
        void 발행이_실패하면_실패한_왕복_시간으로_남긴다() {
            // given
            final StreamPublisher publisher = publisher();
            givenXAddResults(Mono.error(new IllegalStateException("Redis 연결 끊김")));

            // when — 실패 처리가 끝날 때까지 기다린다
            publisher.publishAsync(KEY, 이벤트()).exceptionally(error -> null).join();

            // then — 실패 수는 outcome=error 왕복 시간의 count로 본다
            assertThat(roundTripTimer("error").count()).isEqualTo(1);
        }
    }

    @Nested
    class 실패_처리_컨텍스트 {

        private static final String CONTEXT_KEY = "stream-publisher-test-context";

        private final ThreadLocal<String> requestContext = new ThreadLocal<>();

        @Test
        void 실패_처리는_발행한_스레드의_컨텍스트를_되살려_실행된다() {
            // given — 발행 스레드에만 있는 컨텍스트(트레이스·MDC를 대신하는 ThreadLocal)
            ContextRegistry.getInstance().registerThreadLocalAccessor(CONTEXT_KEY, requestContext);
            try {
                final StreamPublisher publisher = publisher();
                givenXAddResults(Mono.<RecordId>error(new IllegalStateException("Redis 연결 끊김"))
                        .delaySubscription(Duration.ofMillis(200)));

                // when — 발행한 뒤 발행 스레드의 컨텍스트는 지운다
                requestContext.set("trace-1");
                final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());
                requestContext.remove();
                final CompletableFuture<String> contextOnFailure =
                        published.handle((ignored, error) -> requestContext.get());

                // then — 실패를 처리한 다른 스레드에서도 발행 시점의 컨텍스트가 보인다
                assertThat(contextOnFailure)
                        .succeedsWithin(Duration.ofSeconds(2))
                        .isEqualTo("trace-1");
            } finally {
                ContextRegistry.getInstance().removeThreadLocalAccessor(CONTEXT_KEY);
            }
        }

        @Test
        void 실패_알림을_넘기지_못해_지금_스레드에서_알려도_발행한_스레드의_컨텍스트를_되살린다() {
            // given — 실패 알림을 넘길 곳이 작업을 거절하고, XADD 실패는 발행 스레드가 아닌 다른 스레드에서 온다
            ContextRegistry.getInstance().registerThreadLocalAccessor(CONTEXT_KEY, requestContext);
            try {
                final StreamPublisher publisher = publisherWithFailureNotificationExecutor(작업을_거절하는_실행기());
                givenXAddResults(Mono.<RecordId>error(new IllegalStateException("Redis 연결 끊김"))
                        .delaySubscription(Duration.ofMillis(200)));

                // when — 발행한 뒤 발행 스레드의 컨텍스트는 지운다
                requestContext.set("trace-1");
                final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());
                requestContext.remove();
                final CompletableFuture<String> contextOnFailure =
                        published.handle((ignored, error) -> requestContext.get());

                // then — 실패를 받은 스레드에서 바로 알려도 발행 시점의 컨텍스트가 보인다
                assertThat(contextOnFailure)
                        .succeedsWithin(Duration.ofSeconds(2))
                        .isEqualTo("trace-1");
            } finally {
                ContextRegistry.getInstance().removeThreadLocalAccessor(CONTEXT_KEY);
            }
        }
    }
}
