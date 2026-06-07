package coffeeshout.global.redis.config;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.EventDispatcher;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import coffeeshout.global.redis.stream.StreamRecordFields;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@DependsOn("redisStreamThreadPoolConfig")
public class RedisStreamListenerStarter {

    public static final String STREAM_CONTAINER_BEAN_NAME_FORMAT = "stream-container-%s";

    private static final Duration SUBSCRIPTION_START_TIMEOUT = Duration.ofSeconds(5);

    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final List<StreamMessageListenerContainer<String, MapRecord<String, String, String>>> containers =
            new ArrayList<>();
    private final RedisStreamProperties properties;
    private final RedisConnectionFactory redisConnectionFactory;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper redisObjectMapper;
    private final EventDispatcher eventDispatcher;
    private final StreamTracePropagator streamTracePropagator;
    private final ApplicationContext applicationContext;
    private final GenericApplicationContext genericApplicationContext;

    public RedisStreamListenerStarter(
            RedisStreamProperties properties,
            RedisConnectionFactory redisConnectionFactory,
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper,
            EventDispatcher eventDispatcher,
            StreamTracePropagator streamTracePropagator,
            ApplicationContext applicationContext,
            GenericApplicationContext genericApplicationContext
    ) {
        this.properties = properties;
        this.redisConnectionFactory = redisConnectionFactory;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisObjectMapper = redisObjectMapper;
        this.eventDispatcher = eventDispatcher;
        this.streamTracePropagator = streamTracePropagator;
        this.applicationContext = applicationContext;
        this.genericApplicationContext = genericApplicationContext;
    }

    @PostConstruct
    public void streamContainers() {
        if (properties.keys() == null) {
            log.warn("Redis Stream 설정이 없습니다. 리스너를 시작하지 않습니다.");
            return;
        }

        for (Map.Entry<String, StreamConfig> entry : properties.keys().entrySet()) {
            final String streamKey = entry.getKey();
            final StreamConfig streamConfig = entry.getValue();

            final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = createContainer(
                    redisConnectionFactory,
                    findExecutor(streamKey, streamConfig),
                    streamConfig
            );

            containers.add(container);

            final Subscription subscription = container.register(buildReadRequest(streamKey), this::onMessage);
            container.start();
            awaitSubscriptionStart(streamKey, subscription);

            genericApplicationContext.registerBean(
                    String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey),
                    StreamMessageListenerContainer.class,
                    () -> container
            );
        }
    }

    // 폴링 태스크 시작을 보장한 뒤 기동을 완료한다. Stream은 메시지 흐름의 필수 경로이므로
    // 구독 없는 기동은 무의미하다 — 실패 시 fail-fast (ADR-0022)
    void awaitSubscriptionStart(String streamKey, Subscription subscription) {
        try {
            if (!subscription.await(SUBSCRIPTION_START_TIMEOUT)) {
                throw new IllegalStateException(
                        "Redis Stream 구독이 제한 시간 내에 시작되지 않았습니다 (공유 스레드풀 core-size가 "
                                + "스트림 수보다 작은지 확인): " + streamKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Redis Stream 구독 시작 대기 중 인터럽트가 발생했습니다: " + streamKey, e);
        }
    }

    private Executor findExecutor(String streamKey, StreamConfig streamConfig) {
        if (streamConfig.isUseSharedThreadPool()) {
            return applicationContext.getBean(
                    RedisStreamThreadPoolConfig.convertBeanName(streamConfig.threadPoolName()),
                    Executor.class
            );
        }
        return applicationContext.getBean(RedisStreamThreadPoolConfig.convertBeanName(streamKey), Executor.class);
    }

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        stopping.set(true);
    }

    // refresh 실패 시에는 ContextClosedEvent가 발행되지 않아 stopping이 false로 남고,
    // 이미 시작된 컨테이너가 파괴된 커넥션 팩토리에 무한 폴링한다. @PreDestroy는
    // 정상 종료와 refresh 실패 양쪽에서 호출되므로 여기서 확정적으로 멈춘다 (stop()은 멱등)
    @PreDestroy
    public void stopContainers() {
        stopping.set(true);
        containers.forEach(StreamMessageListenerContainer::stop);
    }

    // 종료 신호의 진실 공급원은 stopping 플래그다. 종료 중 폴링 오류는 커넥션 해체 과정의
    // 기대된 노이즈이므로 메시지 텍스트로 세분류하지 않는다 (라이브러리 버전업에 취약)
    private void handleStreamError(Throwable t) {
        if (stopping.get()) {
            log.debug("종료 중 Redis Stream 오류 (정상 종료 과정)", t);
            return;
        }
        log.error("Redis Stream 처리 중 오류가 발생했습니다.", t);
    }

    // receive()의 기본 cancelOnError(t -> true)는 예외 1건으로 구독을 영구 중단시키므로
    // 평시에는 구독을 유지하고, 컨텍스트 종료 중에만 취소를 허용한다
    // (종료 중 취소를 막으면 파괴된 커넥션 팩토리에 폴링을 무한 재시도한다)
    StreamReadRequest<String> buildReadRequest(String streamKey) {
        return StreamReadRequest.builder(StreamOffset.create(streamKey, resolveStartOffset(streamKey)))
                .errorHandler(this::handleStreamError)
                .cancelOnError(t -> stopping.get())
                .build();
    }

    // 기동 시점 스트림의 마지막 ID부터 소비한다 (ADR-0022)
    // - fromStart(0-0): 재시작마다 잔존 메시지 전체를 리플레이한다
    // - latest($): 매 폴링이 $를 재사용해(ReadOffsetStrategy.Latest) 폴링 사이 발행분을 상시 유실한다
    // 구체 ID는 NextMessage 전략을 타므로 리플레이 없이 이후 메시지를 빠짐없이 소비하며,
    // 오프셋이 등록 시점에 고정되어 첫 XREAD 전에 발행된 메시지도 수신된다
    private ReadOffset resolveStartOffset(String streamKey) {
        final List<MapRecord<String, Object, Object>> lastRecords = stringRedisTemplate.opsForStream()
                .reverseRange(streamKey, Range.unbounded(), Limit.limit().count(1));
        if (lastRecords == null || lastRecords.isEmpty()) {
            return ReadOffset.from("0-0");
        }
        return ReadOffset.from(lastRecords.getFirst().getId());
    }

    void onMessage(MapRecord<String, String, String> message) {
        final Map<String, String> fields = message.getValue();
        final String payload = resolvePayload(fields);
        if (payload == null) {
            log.error("payload 필드가 없는 레코드입니다: stream={}, recordId={}", message.getStream(), message.getId());
            return;
        }
        try {
            final BaseEvent event = redisObjectMapper.readValue(payload, BaseEvent.class);
            streamTracePropagator.runInConsumerScope(
                    fields,
                    event.getClass().getSimpleName(),
                    () -> eventDispatcher.handle(event)
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to parse event: {}", payload, e);
        } catch (Exception e) {
            // EventDispatcher가 내부에서 예외를 삼키므로 평소엔 도달하지 않는 최후 안전망.
            // 재던지면 구독이 cancel될 수 있으므로 로깅만 한다 (컨슈머 그룹 미사용 — 재전달 이득 없음)
            log.error("예외가 발생했습니다.", e);
        }
    }

    private String resolvePayload(Map<String, String> fields) {
        final String payload = fields.get(StreamRecordFields.PAYLOAD);
        if (payload != null) {
            return payload;
        }
        // 구형 ObjectRecord 포맷 폴백 — 1릴리스 유지 후 제거
        return fields.get(StreamRecordFields.LEGACY_RAW);
    }

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> createContainer(
            RedisConnectionFactory redisConnectionFactory,
            Executor executor,
            StreamConfig streamConfig
    ) {
        if (properties.commonSettings() == null) {
            throw new IllegalStateException("Redis Stream 공통 설정이 없습니다.");
        }

        // options 레벨 errorHandler는 등록하지 않는다 — read request에 없을 때의 폴백일 뿐이며,
        // 모든 구독은 buildReadRequest()를 경유해 errorHandler와 cancelOnError를 함께 설정한다 (ADR-0022)
        final StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .batchSize(streamConfig.getBatchSize(properties.commonSettings()))
                        .executor(executor)
                        .pollTimeout(streamConfig.getPollTimeout(properties.commonSettings()))
                        .build();

        return StreamMessageListenerContainer.create(redisConnectionFactory, options);
    }
}
