package coffeeshout.settlement.infra.consumer;

import coffeeshout.global.redis.config.RedisStreamContainerRegistry;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.settlement.infra.SettlementStreamKey;
import coffeeshout.settlement.infra.consumer.SettlementMessageProcessor.PoisonMessageException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.ConsumerStreamReadRequest;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 정산 스트림의 컨슈머 그룹 소비자. ZZOL 최초의 XREADGROUP 경로다(#1610).
 * <p>
 * 브로드캐스트 리스너({@code RedisStreamListenerStarter})와의 차이:
 * <ul>
 *   <li>그룹 내 한 컨슈머만 메시지를 받는다 — blue/green 두 인스턴스가 떠 있어도
 *       정산은 정확히 한 번 일어난다.</li>
 *   <li>처리 성공 시에만 XACK한다. ACK 전에 죽으면 메시지가 PEL에 남고,
 *       {@code SettlementPendingSweeper}가 회수해 다른 인스턴스에서 재처리한다.</li>
 *   <li>시작 오프셋은 XREVRANGE 해석이 아니라 그룹의 last-delivered-id다
 *       (ReadOffset.lastConsumed) — 서버가 오프셋을 기억하므로 재시작 유실이 없다.</li>
 * </ul>
 * 컨테이너는 {@code RedisStreamContainerRegistry}에 등록해 기존 SmartLifecycle
 * 정지·복구(RedisStreamContainerRecovery) 체계에 편입한다.
 */
@Slf4j
@Component
public class SettlementStreamConsumer {

    static final String STREAM_KEY = SettlementStreamKey.RESULT.getRedisKey();
    static final String GROUP = "settlement";

    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final RedisConnectionFactory redisConnectionFactory;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisStreamProperties properties;
    private final RedisStreamContainerRegistry containerRegistry;
    private final SettlementMessageProcessor processor;
    private final SettlementDeadLetterPublisher deadLetterPublisher;
    private final ThreadPoolTaskExecutor executor;
    private final String consumerName;

    public SettlementStreamConsumer(
            RedisConnectionFactory redisConnectionFactory,
            StringRedisTemplate stringRedisTemplate,
            RedisStreamProperties properties,
            RedisStreamContainerRegistry containerRegistry,
            SettlementMessageProcessor processor,
            SettlementDeadLetterPublisher deadLetterPublisher,
            @Qualifier("settlementConsumerExecutor") ThreadPoolTaskExecutor executor,
            @Qualifier("settlementConsumerName") String consumerName
    ) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
        this.containerRegistry = containerRegistry;
        this.processor = processor;
        this.deadLetterPublisher = deadLetterPublisher;
        this.executor = executor;
        this.consumerName = consumerName;
    }

    @PostConstruct
    public void start() {
        ensureGroup();

        final StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .batchSize(properties.commonSettings().batchSize())
                        .executor(executor)
                        .pollTimeout(properties.commonSettings().pollTimeout())
                        .build();
        final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        // start/await 이전에 등록 — 기동 실패 시에도 레지스트리의 stop 대상에 포함된다 (ADR-0022)
        containerRegistry.register(STREAM_KEY, container);

        final Subscription subscription = container.register(buildReadRequest(), this::onMessage);
        container.start();
        awaitSubscriptionStart(subscription);
        log.info("정산 컨슈머 그룹 구독 시작: stream={}, group={}, consumer={}", STREAM_KEY, GROUP, consumerName);
    }

    /**
     * 그룹을 0-0부터 생성한다(MKSTREAM). 그룹 생성 전에 발행된 메시지도 처리 대상이다 —
     * 브로드캐스트 스트림과 달리 이 스트림의 목적은 처리 보장이기 때문이다.
     * 이미 있으면(BUSYGROUP) 정상 경로다.
     */
    private void ensureGroup() {
        try {
            final byte[] rawKey = RedisSerializer.string().serialize(STREAM_KEY);
            stringRedisTemplate.execute(connection -> {
                connection.streamCommands().xGroupCreate(rawKey, GROUP, ReadOffset.from("0-0"), true);
                return null;
            }, true);
        } catch (RedisSystemException e) {
            if (e.getCause() != null && String.valueOf(e.getCause().getMessage()).contains("BUSYGROUP")) {
                log.debug("정산 컨슈머 그룹이 이미 존재합니다: {}", GROUP);
                return;
            }
            throw e;
        }
    }

    ConsumerStreamReadRequest<String> buildReadRequest() {
        return ConsumerStreamReadRequest
                .builder(StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()))
                .consumer(Consumer.from(GROUP, consumerName))
                // ACK는 처리 성공 후 명시적으로 — 자동 ACK는 처리 실패 시에도 PEL에서 지워져 회수가 불가능하다
                .autoAcknowledge(false)
                .errorHandler(this::handleStreamError)
                .cancelOnError(t -> stopping.get())
                .build();
    }

    void onMessage(MapRecord<String, String, String> record) {
        try {
            processor.process(record);
            acknowledge(record);
        } catch (PoisonMessageException e) {
            // 재시도해도 영원히 실패한다 — 격리하고 ACK해 재전달 루프를 끊는다
            deadLetterPublisher.publish(record, e.getMessage());
            acknowledge(record);
        } catch (Exception e) {
            // ACK하지 않는다 — PEL에 남아 스위퍼가 재전달한다. 멱등성은 정산 원장이 보장한다.
            log.error("정산 처리 실패 — ACK 보류, 재전달 대기: recordId={}", record.getId(), e);
        }
    }

    private void acknowledge(MapRecord<String, String, String> record) {
        stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
    }

    private void awaitSubscriptionStart(Subscription subscription) {
        final Duration timeout = properties.commonSettings().subscriptionStartTimeout();
        try {
            if (!subscription.await(timeout)) {
                throw new IllegalStateException("정산 컨슈머 그룹 구독이 제한 시간 내에 시작되지 않았습니다: " + STREAM_KEY);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("정산 컨슈머 그룹 구독 시작 대기 중 인터럽트: " + STREAM_KEY, e);
        }
    }

    private void handleStreamError(Throwable t) {
        if (stopping.get()) {
            log.debug("종료 중 정산 스트림 오류 (정상 종료 과정): {}", t.getMessage());
            return;
        }
        // 그룹이 사라진 경우(AOF 없는 Redis 재시작·flush 등) 자가 복구한다. 그룹은 0-0부터
        // 재생성되므로 스트림에 남아 있는 메시지는 다시 소비된다 — 멱등성은 정산 원장이 보장한다.
        if (String.valueOf(t.getMessage()).contains("NOGROUP")) {
            log.warn("정산 컨슈머 그룹이 존재하지 않아 재생성합니다: {}", GROUP);
            ensureGroup();
            return;
        }
        log.error("정산 스트림 처리 중 오류가 발생했습니다.", t);
    }

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        stopping.set(true);
    }

    @PreDestroy
    public void markStopping() {
        stopping.set(true);
    }
}
