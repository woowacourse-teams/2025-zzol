package coffeeshout.global.redis.config;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.EventDispatcher;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import coffeeshout.global.redis.stream.StreamRecordFields;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@DependsOn("redisStreamThreadPoolConfig")
public class RedisStreamListenerStarter {

    public static final String STREAM_CONTAINER_BEAN_NAME_FORMAT = "stream-container-%s";

    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final RedisStreamProperties properties;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ObjectMapper redisObjectMapper;
    private final EventDispatcher eventDispatcher;
    private final StreamTracePropagator streamTracePropagator;
    private final ApplicationContext applicationContext;
    private final GenericApplicationContext genericApplicationContext;

    public RedisStreamListenerStarter(
            RedisStreamProperties properties,
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper,
            EventDispatcher eventDispatcher,
            StreamTracePropagator streamTracePropagator,
            ApplicationContext applicationContext,
            GenericApplicationContext genericApplicationContext
    ) {
        this.properties = properties;
        this.redisConnectionFactory = redisConnectionFactory;
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

            container.receive(StreamOffset.fromStart(streamKey), this::onMessage);

            genericApplicationContext.registerBean(
                    String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey),
                    StreamMessageListenerContainer.class,
                    () -> container
            );
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

    private void handleStreamError(Throwable t) {
        if (stopping.get() && isShutdownRelated(t)) {
            log.debug("Redis Stream 연결이 종료됐습니다 (정상 종료)");
            return;
        }
        log.error("Redis Stream 처리 중 오류가 발생했습니다.", t);
    }

    private static final String CONNECTION_CLOSED = "Connection closed";
    private static final String FACTORY_STOPPING = "is STOPPING";
    private static final String FACTORY_STOPPED = "has been STOPPED";

    private boolean isShutdownRelated(Throwable t) {
        Throwable current = t;
        while (current != null) {
            final String msg = current.getMessage();
            if (msg != null && (msg.contains(CONNECTION_CLOSED) || msg.contains(FACTORY_STOPPING) || msg.contains(FACTORY_STOPPED))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void onMessage(MapRecord<String, String, String> message) {
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
            log.error("예외가 발생했습니다.", e);
            throw e;
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

        final StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .batchSize(streamConfig.getBatchSize(properties.commonSettings()))
                        .executor(executor)
                        .pollTimeout(streamConfig.getPollTimeout(properties.commonSettings()))
                        .errorHandler(this::handleStreamError)
                        .build();

        final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        container.start();

        return container;
    }
}
