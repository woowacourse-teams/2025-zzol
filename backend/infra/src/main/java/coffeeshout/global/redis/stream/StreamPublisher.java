package coffeeshout.global.redis.stream;

import coffeeshout.global.metric.RedisStreamPublishMetricService;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.config.RedisStreamProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.Timer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisStreamCommands.TrimOptions;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
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
    private final RedisStreamPublishMetricService publishMetricService;
    private final ContextSnapshotFactory contextSnapshotFactory;
    private final Executor failureNotificationExecutor;

    @Autowired
    public StreamPublisher(
            StringRedisTemplate stringRedisTemplate,
            ReactiveStringRedisTemplate reactiveStringRedisTemplate,
            RedisStreamProperties redisStreamProperties,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            StreamTracePropagator streamTracePropagator,
            RedisStreamPublishMetricService publishMetricService,
            ContextSnapshotFactory contextSnapshotFactory) {
        this(
                stringRedisTemplate,
                reactiveStringRedisTemplate,
                redisStreamProperties,
                objectMapper,
                streamTracePropagator,
                publishMetricService,
                contextSnapshotFactory,
                OFF_EVENT_LOOP);
    }

    // 실패 알림을 넘길 곳을 바꿔 끼워, 넘기지 못한 경우를 테스트한다
    StreamPublisher(
            StringRedisTemplate stringRedisTemplate,
            ReactiveStringRedisTemplate reactiveStringRedisTemplate,
            RedisStreamProperties redisStreamProperties,
            ObjectMapper objectMapper,
            StreamTracePropagator streamTracePropagator,
            RedisStreamPublishMetricService publishMetricService,
            ContextSnapshotFactory contextSnapshotFactory,
            Executor failureNotificationExecutor) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
        this.redisStreamProperties = redisStreamProperties;
        this.objectMapper = objectMapper;
        this.streamTracePropagator = streamTracePropagator;
        this.publishMetricService = publishMetricService;
        this.contextSnapshotFactory = contextSnapshotFactory;
        this.failureNotificationExecutor = failureNotificationExecutor;
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
     * <p>
     * 응답을 기다리는 명령 수의 상한은 {@code RedisConfig}가 정한 요청 큐 크기로 Lettuce가 지킨다. 상한을 넘으면
     * Lettuce가 명령을 보내지 않고 바로 실패시키고, 그 실패도 다른 발행 실패와 같은 방식으로 사용자에게 알린다.
     */
    public CompletableFuture<Void> publishAsync(StreamKey key, BaseEvent event) {
        final String redisKey = key.getRedisKey();
        final MapRecord<String, String, String> record =
                StreamRecords.newRecord().in(redisKey).ofMap(toFields(event));
        final XAddOptions options = xAddOptions(redisKey);

        final Timer.Sample roundTrip = publishMetricService.startRoundTrip();
        final ContextSnapshot publishContext = contextSnapshotFactory.captureAll();
        // thenXxx로 이어 만든 future는 실패를 CompletionException으로 감싸고, Spring은 이를 풀지 않은 채
        // @MessageExceptionHandler를 고른다. 원래 예외가 그대로 가도록 future를 직접 만들어 완료한다
        final CompletableFuture<Void> published = new CompletableFuture<>();
        // defer: add()가 Mono를 만들다 바로 던져도 오류 신호로 바뀌어, 다른 실패와 같은 경로로 지표를 남기고 알린다
        Mono.defer(() -> reactiveStringRedisTemplate.opsForStream().add(record, options))
                .subscribe(
                        recordId -> {},
                        error -> {
                            publishMetricService.recordFailed(roundTrip, redisKey);
                            notifyFailure(publishContext, published, error);
                        },
                        () -> {
                            publishMetricService.recordPublished(roundTrip, redisKey);
                            published.complete(null);
                        });
        return published;
    }

    /**
     * 실패 알림을 이벤트 루프 밖으로 넘긴다. 스케줄러가 가득 찼거나 멈춰 넘기지 못하면, 알림이 사라지지 않도록
     * 지금 스레드에서 바로 알린다. 어느 쪽이든 발행 시점의 컨텍스트를 되살려 실행한다.
     */
    private void notifyFailure(ContextSnapshot publishContext, CompletableFuture<Void> published, Throwable error) {
        final Runnable failPublished = () -> published.completeExceptionally(error);
        final Runnable notification = publishContext.wrap(failPublished);
        try {
            failureNotificationExecutor.execute(notification);
        } catch (RejectedExecutionException rejected) {
            log.warn("Redis Stream 비동기 발행의 실패 알림을 넘기지 못해 지금 스레드에서 알립니다", rejected);
            notification.run();
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
        stringRedisTemplate
                .opsForStream()
                .add(StreamRecords.newRecord().in(redisKey).ofMap(fields), xAddOptions(redisKey));
        publishMetricService.countPublished(redisKey);
    }

    private XAddOptions xAddOptions(String redisKey) {
        return XAddOptions.trim(
                TrimOptions.maxLen(redisStreamProperties.maxLength(redisKey)).approximate());
    }
}
