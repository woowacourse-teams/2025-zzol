package coffeeshout.global.redis.stream;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import coffeeshout.fixture.BaseEventDummy;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.metric.RedisStreamPublishMetricService;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.CommonSettings;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import coffeeshout.global.redis.config.StreamPublishProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final RedisStreamPublishMetricService metricService = new RedisStreamPublishMetricService(meterRegistry);

    private InFlightPublishLimiter limiter(int maxInFlight, Duration drainTimeout) {
        return new InFlightPublishLimiter(new StreamPublishProperties(maxInFlight, drainTimeout), metricService);
    }

    private StreamPublisher publisherWithMaxInFlight(int maxInFlight) {
        return publisherWith(limiter(maxInFlight, Duration.ofSeconds(5)));
    }

    private StreamPublisher publisherWith(InFlightPublishLimiter limiter) {
        final ReactiveStringRedisTemplate reactiveTemplate = mock(ReactiveStringRedisTemplate.class);
        doReturn(reactiveStreamOperations).when(reactiveTemplate).opsForStream();

        final RedisStreamProperties redisStreamProperties = new RedisStreamProperties(
                new CommonSettings(100, 10, Duration.ofSeconds(2), Duration.ofSeconds(5)),
                Map.of(),
                Map.of("room", new StreamConfig("concurrent", null, null, null, null, null)));

        return new StreamPublisher(
                mock(StringRedisTemplate.class),
                reactiveTemplate,
                redisStreamProperties,
                limiter,
                new ObjectMapper().findAndRegisterModules(),
                mock(StreamTracePropagator.class),
                meterRegistry,
                metricService,
                ContextSnapshotFactory.builder().build());
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

    private double rejectedCount() {
        return meterRegistry
                .counter(RedisStreamPublishMetricService.REJECTED_METRIC, "stream", "room")
                .count();
    }

    private BaseEventDummy 이벤트() {
        return BaseEventDummy.페이로드("비동기 발행");
    }

    @Nested
    class 비동기_발행 {

        @Test
        void XADD_응답을_기다리지_않고_바로_돌아온다() {
            // given — 응답이 끝내 오지 않는 XADD
            final StreamPublisher publisher = publisherWithMaxInFlight(10);
            givenXAddResults(Mono.never());

            // when
            final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());

            // then — 호출 스레드는 응답을 기다리지 않고 돌아오고, 결과는 아직 정해지지 않았다
            assertThat(published).isNotDone();
        }

        @Test
        void XADD가_끝나면_돌려준_future가_완료된다() {
            // given
            final StreamPublisher publisher = publisherWithMaxInFlight(10);
            givenXAddResults(Mono.just(RecordId.of("1-0")));

            // when
            final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());

            // then
            assertThat(published).succeedsWithin(Duration.ofSeconds(1));
        }

        @Test
        void 발행이_실패하면_future가_XADD_예외_그대로_완료되고_자리를_돌려준다() {
            // given — 상한 1, 첫 XADD는 실패
            final StreamPublisher publisher = publisherWithMaxInFlight(1);
            final IllegalStateException failure = new IllegalStateException("Redis 연결 끊김");
            givenXAddResults(Mono.error(failure), Mono.never());

            // when
            final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());

            // then — Spring이 예외 타입으로 @MessageExceptionHandler를 고르므로 CompletionException으로 감싸지 않은
            // 원래 예외를 받는다. 실패한 발행은 자리를 돌려줘 다음 발행이 거절되지 않는다
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(published.handle((ignored, error) -> error))
                        .succeedsWithin(Duration.ofSeconds(1))
                        .isSameAs(failure);
                softly.assertThatCode(() -> publisher.publishAsync(KEY, 이벤트())).doesNotThrowAnyException();
            });
        }

        @Test
        void 응답을_기다리는_발행이_상한에_닿으면_거절한다() {
            // given — 상한 1, 응답이 오지 않아 첫 발행이 자리를 계속 차지한다
            final StreamPublisher publisher = publisherWithMaxInFlight(1);
            givenXAddResults(Mono.never());
            publisher.publishAsync(KEY, 이벤트());

            // when & then
            assertCoffeeShoutException(
                    () -> publisher.publishAsync(KEY, 이벤트()), GlobalErrorCode.STREAM_PUBLISH_OVERLOADED);
        }

        @Test
        void XADD_명령을_만들다_바로_실패해도_future로_알리고_자리를_돌려준다() {
            // given — 상한 1, Mono를 돌려주기 전에 바로 던지는 XADD
            final StreamPublisher publisher = publisherWithMaxInFlight(1);
            final IllegalStateException failure = new IllegalStateException("커넥션 없음");
            given(reactiveStreamOperations.add(any(MapRecord.class), any(XAddOptions.class)))
                    .willThrow(failure)
                    .willReturn(Mono.never());

            // when
            final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());

            // then — 호출 스레드로 예외가 새지 않고 future가 실패하며, 자리를 돌려줘 다음 발행이 거절되지 않는다
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(published.handle((ignored, error) -> error))
                        .succeedsWithin(Duration.ofSeconds(1))
                        .isSameAs(failure);
                softly.assertThatCode(() -> publisher.publishAsync(KEY, 이벤트())).doesNotThrowAnyException();
            });
        }
    }

    @Nested
    class 지표 {

        @Test
        void 호출_스레드가_기다리지_않아도_XADD_왕복_시간을_잰다() {
            // given — 100ms 뒤에 응답하는 XADD
            final StreamPublisher publisher = publisherWithMaxInFlight(10);
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
            final StreamPublisher publisher = publisherWithMaxInFlight(10);
            givenXAddResults(Mono.error(new IllegalStateException("Redis 연결 끊김")));

            // when — 실패 처리가 끝날 때까지 기다린다
            publisher.publishAsync(KEY, 이벤트()).exceptionally(error -> null).join();

            // then — 실패 수는 outcome=error 왕복 시간의 count로 본다. 거절 수와 섞이지 않는다
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(roundTripTimer("error").count()).isEqualTo(1);
                softly.assertThat(rejectedCount()).isZero();
            });
        }

        @Test
        void 상한에_닿아_거절하면_거절_수를_남긴다() {
            // given — 상한 1, 첫 발행이 자리를 계속 차지한다
            final StreamPublisher publisher = publisherWithMaxInFlight(1);
            givenXAddResults(Mono.never());
            publisher.publishAsync(KEY, 이벤트());

            // when
            catchThrowable(() -> publisher.publishAsync(KEY, 이벤트()));

            // then
            assertThat(rejectedCount()).isEqualTo(1.0);
        }

        @Test
        void 응답을_기다리는_발행_수를_보여준다() {
            // given — 응답이 오지 않는 XADD 두 건
            final StreamPublisher publisher = publisherWithMaxInFlight(10);
            givenXAddResults(Mono.never());
            publisher.publishAsync(KEY, 이벤트());
            publisher.publishAsync(KEY, 이벤트());

            // when & then
            assertThat(meterRegistry
                            .get(RedisStreamPublishMetricService.IN_FLIGHT_METRIC)
                            .gauge()
                            .value())
                    .isEqualTo(2.0);
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
                final StreamPublisher publisher = publisherWithMaxInFlight(10);
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
    }

    @Nested
    class 종료 {

        @Test
        void 응답을_기다리는_발행이_끝날_때까지_기다린다() {
            // given — 200ms 뒤에 끝나는 XADD
            final InFlightPublishLimiter limiter = limiter(10, Duration.ofSeconds(2));
            final StreamPublisher publisher = publisherWith(limiter);
            givenXAddResults(Mono.just(RecordId.of("1-0")).delayElement(Duration.ofMillis(200)));
            final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());

            // when
            limiter.stop();

            // then — 다 비운 뒤에 돌아온다
            assertThat(published).isDone();
        }

        @Test
        void 제한_시간_안에_끝나지_않으면_기다리기를_멈춘다() {
            // given — 끝나지 않는 XADD, 종료 대기 한도 100ms
            final InFlightPublishLimiter limiter = limiter(10, Duration.ofMillis(100));
            final StreamPublisher publisher = publisherWith(limiter);
            givenXAddResults(Mono.never());
            final CompletableFuture<Void> published = publisher.publishAsync(KEY, 이벤트());

            // when & then — 한도가 지나면 남은 발행을 두고 돌아온다
            assertThat(CompletableFuture.runAsync(limiter::stop)).succeedsWithin(Duration.ofSeconds(2));
            assertThat(published).isNotDone();
        }

        @Test
        void 멈췄다가_다시_시작하면_발행을_받는다() {
            // given — 테스트 컨텍스트처럼 lifecycle이 멈췄다가 다시 시작한다
            final InFlightPublishLimiter limiter = limiter(1, Duration.ofSeconds(1));
            final StreamPublisher publisher = publisherWith(limiter);
            givenXAddResults(Mono.just(RecordId.of("1-0")));
            limiter.stop();

            // when
            limiter.start();

            // then — 종료 대기에 잡았던 자리를 돌려줘, 상한 1에서도 발행이 거절되지 않는다
            assertThat(publisher.publishAsync(KEY, 이벤트())).succeedsWithin(Duration.ofSeconds(1));
        }
    }
}
