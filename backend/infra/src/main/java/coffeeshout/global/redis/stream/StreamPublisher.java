package coffeeshout.global.redis.stream;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.global.metric.RedisStreamPublishMetricService;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import coffeeshout.global.redis.config.StreamPublishProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisStreamCommands.TrimOptions;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class StreamPublisher {

    /**
     * 비동기 발행의 실패 알림을 실행할 곳. 실패를 받은 Spring이 이어서 사용자에게 에러를 보내는 일이
     * 응답을 받은 Lettuce 이벤트 루프를 붙잡지 않게 한다.
     */
    private static final Executor OFF_EVENT_LOOP =
            task -> Schedulers.boundedElastic().schedule(task);

    private final StringRedisTemplate stringRedisTemplate;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private final RedisStreamProperties redisStreamProperties;
    private final ObjectMapper objectMapper;
    private final StreamTracePropagator streamTracePropagator;
    private final MeterRegistry meterRegistry;
    private final RedisStreamPublishMetricService publishMetricService;
    private final ContextSnapshotFactory contextSnapshotFactory;
    private final int maxInFlight;
    private final Duration drainTimeout;
    private final Semaphore asyncInFlight;
    // 발행마다 Counter를 새로 만들지 않도록 스트림별로 캐싱한다 (WebSocketMetricService 선례)
    private final Map<String, Counter> publishedCounters = new ConcurrentHashMap<>();

    public StreamPublisher(
            StringRedisTemplate stringRedisTemplate,
            ReactiveStringRedisTemplate reactiveStringRedisTemplate,
            RedisStreamProperties redisStreamProperties,
            StreamPublishProperties streamPublishProperties,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            StreamTracePropagator streamTracePropagator,
            MeterRegistry meterRegistry,
            RedisStreamPublishMetricService publishMetricService,
            ContextSnapshotFactory contextSnapshotFactory) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
        this.redisStreamProperties = redisStreamProperties;
        this.objectMapper = objectMapper;
        this.streamTracePropagator = streamTracePropagator;
        this.meterRegistry = meterRegistry;
        this.publishMetricService = publishMetricService;
        this.contextSnapshotFactory = contextSnapshotFactory;
        this.maxInFlight = streamPublishProperties.maxInFlight();
        this.drainTimeout = streamPublishProperties.drainTimeout();
        this.asyncInFlight = new Semaphore(maxInFlight);
        publishMetricService.bindInFlight(asyncInFlight, maxInFlight);
    }

    // 카운터를 기동 시점에 0으로 만들어 둔다. 첫 발행 때 시계열이 생기면 Prometheus rate()가 그
    // 첫 샘플을 증가로 세지 않아, 기동 후 발행이 한 번뿐인 스트림은 컨슈머가 멎어도 정지 룰이
    // 침묵한다. 발행이 띄엄한 스트림을 잡는 게 그 룰의 목적이다.
    @PostConstruct
    public void initializeCounters() {
        if (redisStreamProperties.keys() == null) {
            return;
        }
        redisStreamProperties
                .keys()
                .keySet()
                .forEach(redisKey -> publishedCounters.computeIfAbsent(redisKey, this::registerPublishedCounter));
    }

    public void publish(StreamKey key, BaseEvent event) {
        publish(key.getRedisKey(), event);
    }

    /**
     * 이벤트를 직렬화하고 현재 트레이스 컨텍스트를 캐리어 필드로 주입해 발행한다.
     */
    public void publish(String redisKey, BaseEvent event) {
        publishFields(redisKey, toFields(event));
    }

    /**
     * 이미 직렬화된 페이로드를 명시된 traceparent와 함께 발행한다.
     * <p>
     * Outbox 릴레이처럼 발행 스레드에 원본 트레이스 컨텍스트가 없는 경로에서 사용한다.
     */
    public void publish(String redisKey, String payload, String traceparent) {
        final Map<String, String> fields = new HashMap<>();
        fields.put(StreamRecordFields.PAYLOAD, payload);
        if (traceparent != null && !traceparent.isBlank()) {
            fields.put(StreamRecordFields.TRACEPARENT, traceparent);
        }
        publishFields(redisKey, fields);
    }

    /**
     * XADD 응답을 기다리지 않고 발행한다. 호출 스레드는 명령을 Lettuce 이벤트 루프에 넘기고 바로 돌아온다.
     * <p>
     * 클라이언트 응답을 Stream 컨슈머가 보내는 WebSocket 인바운드 핸들러처럼, 발행 결과를 쓰지 않는 경로 전용이다.
     * 발행 성공을 확인해야 하는 경로(Outbox 릴레이 등)는 {@link #publish(String, String, String)}를 쓴다.
     * 직렬화와 트레이스 주입은 호출 스레드에서 끝낸다.
     * <p>
     * 돌려주는 future는 XADD가 끝나면 완료되고, 실패하면 XADD의 예외 그대로 완료된다. {@code @MessageMapping} 핸들러가
     * 이 future를 반환하면 Spring이 실패를 {@code @MessageExceptionHandler}로 넘겨, 동기 발행 때처럼 보낸 사용자에게
     * 에러를 보내고 에러 로그를 남긴다. 이 일은 이벤트 루프 밖에서 발행 시점의 컨텍스트(트레이스, MDC)를 되살려
     * 실행하므로, 그 로그에도 요청의 traceId가 남는다.
     *
     * @throws InfrastructureException 응답을 기다리는 비동기 발행이 상한에 닿았을 때
     */
    public CompletableFuture<Void> publishAsync(StreamKey key, BaseEvent event) {
        final String redisKey = key.getRedisKey();
        final Map<String, String> fields = toFields(event);
        final Optional<XAddOptions> options = resolveXAddOptions(redisKey);
        if (options.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        if (!asyncInFlight.tryAcquire()) {
            publishMetricService.recordRejected(redisKey);
            throw new InfrastructureException(
                    GlobalErrorCode.STREAM_PUBLISH_OVERLOADED, "응답을 기다리는 Redis Stream 발행이 상한에 닿았습니다: " + redisKey);
        }

        final Timer.Sample roundTrip = publishMetricService.startRoundTrip();
        final Executor failureNotifier = contextSnapshotFactory.captureAll().wrapExecutor(OFF_EVENT_LOOP);
        // thenXxx로 이어 만든 future는 실패를 CompletionException으로 감싸고, Spring은 이를 풀지 않은 채
        // @MessageExceptionHandler를 고른다. 원래 예외가 그대로 가도록 future를 직접 만들어 완료한다
        final CompletableFuture<Void> published = new CompletableFuture<>();
        // defer: add()가 Mono를 만들다 바로 던져도 오류 신호로 바뀌어, doFinally가 자리를 돌려준다
        Mono.defer(() -> reactiveStringRedisTemplate
                        .opsForStream()
                        .add(StreamRecords.newRecord().in(redisKey).ofMap(fields), options.get()))
                .doFinally(signal -> asyncInFlight.release())
                .subscribe(
                        recordId -> {},
                        error -> {
                            publishMetricService.recordFailed(roundTrip, redisKey);
                            failureNotifier.execute(() -> published.completeExceptionally(error));
                        },
                        () -> {
                            publishMetricService.recordPublished(roundTrip, redisKey);
                            countPublished(redisKey);
                            published.complete(null);
                        });
        return published;
    }

    /**
     * 종료할 때 이미 넘긴 비동기 발행이 끝나기를 기다린다. 이 빈은 Redis 커넥션 팩토리보다 먼저 정리되므로,
     * 커넥션이 닫히기 전에 대기 중인 XADD가 끝난다. 기다린 뒤에는 자리를 돌려주지 않아 새 비동기 발행을 받지 않는다.
     */
    @PreDestroy
    void awaitInFlightPublishes() {
        drainInFlight(drainTimeout);
    }

    boolean drainInFlight(Duration timeout) {
        try {
            if (asyncInFlight.tryAcquire(maxInFlight, timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                return true;
            }
            log.warn("종료 전에 끝나지 않은 Redis Stream 비동기 발행이 있습니다: {}건", maxInFlight - asyncInFlight.availablePermits());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private Map<String, String> toFields(BaseEvent event) {
        final Map<String, String> fields = new HashMap<>();
        try {
            fields.put(StreamRecordFields.PAYLOAD, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("직렬화 실패: " + e.getMessage(), e);
        }
        streamTracePropagator.injectCurrentContext(fields);
        return fields;
    }

    private void publishFields(String redisKey, Map<String, String> fields) {
        resolveXAddOptions(redisKey).ifPresent(options -> {
            stringRedisTemplate
                    .opsForStream()
                    .add(StreamRecords.newRecord().in(redisKey).ofMap(fields), options);
            countPublished(redisKey);
        });
    }

    /**
     * 스트림 설정을 확인하고 XADD 옵션을 만든다. 설정이 아예 없으면 발행하지 않으므로 비어 있는 값을 돌려준다.
     */
    private Optional<XAddOptions> resolveXAddOptions(String redisKey) {
        if (redisStreamProperties.keys() == null) {
            log.warn("Redis Stream 설정이 없습니다. 이벤트를 발행하지 않습니다: {}", redisKey);
            return Optional.empty();
        }

        if (!redisStreamProperties.keys().containsKey(redisKey)) {
            log.error(
                    "존재하지 않는 키입니다: {}. 사용 가능한 키: {}",
                    redisKey,
                    redisStreamProperties.keys().keySet());
            throw new IllegalArgumentException("존재하지 않는 키입니다: " + redisKey);
        }

        final StreamConfig streamConfig = redisStreamProperties.keys().get(redisKey);

        if (redisStreamProperties.commonSettings() == null) {
            log.warn("Redis Stream 공통 설정이 없습니다. 이벤트를 발행하지 않습니다: {}", redisKey);
            return Optional.empty();
        }

        final int maxLength = streamConfig.getMaxLength(redisStreamProperties.commonSettings());
        return Optional.of(XAddOptions.trim(TrimOptions.maxLen(maxLength).approximate()));
    }

    private void countPublished(String redisKey) {
        publishedCounters
                .computeIfAbsent(redisKey, this::registerPublishedCounter)
                .increment();
    }

    // 발행률만으로는 적체를 알 수 없다. 소비율(redis_stream_consumed_total)과 짝지어
    // "발행은 되는데 소비가 0"인 상태를 잡는 데 쓴다 (RedisStreamConsumptionStalled, #1744).
    private Counter registerPublishedCounter(String redisKey) {
        return Counter.builder("redis.stream.published")
                .description("Redis Stream에 발행된 메시지 수")
                .tag("stream", redisKey)
                .register(meterRegistry);
    }
}
