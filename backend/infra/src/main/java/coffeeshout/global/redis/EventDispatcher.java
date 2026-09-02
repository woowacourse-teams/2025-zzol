package coffeeshout.global.redis;

import coffeeshout.global.metric.RedisStreamLatencyMetricService;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventDispatcher {

    private final ApplicationContext applicationContext;
    private final RedisStreamLatencyMetricService latencyMetricService;
    private final RedisStreamProperties redisStreamProperties;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> consumedCounters = new ConcurrentHashMap<>();

    /**
     * 브로드캐스트 리스너가 있는 스트림만 소비 카운터를 만든다. 이 등록 범위가 컨슈머 정지 알림
     * ({@code RedisStreamConsumptionStalled})의 판정 근거다. 소비 경로가 없는 스트림
     * ({@code listener-enabled: false})은 시계열이 생기지 않아 그 룰의 대상에서 빠지므로, 룰에
     * 스트림 이름을 박아 예외를 두지 않아도 설정이 바뀌면 대상이 따라 바뀐다(#1744).
     *
     * <p>기동 시점에 0으로 만들어 두는 이유는 Prometheus {@code rate()}가 시계열의 첫 샘플을
     * 증가로 세지 않기 때문이다. 첫 소비 때 시계열이 생기면 그 한 건이 소비율에 안 잡힌다.</p>
     */
    @PostConstruct
    public void initializeCounters() {
        if (redisStreamProperties.keys() == null) {
            log.warn("Redis Stream 설정이 없습니다. 소비 카운터를 미리 등록하지 않습니다.");
            return;
        }

        for (Map.Entry<String, StreamConfig> entry :
                redisStreamProperties.keys().entrySet()) {
            if (entry.getValue().isListenerEnabled()) {
                consumedCounters.computeIfAbsent(entry.getKey(), this::registerConsumedCounter);
            }
        }
    }

    // 동일 이벤트 타입의 Consumer 전체에 팬아웃한다 (ADR-0025 결정 6 — 예: RoomLifecycleEvent.Created를
    // RoomCreateConsumer와 GameSessionInitConsumer가 함께 처리). 한 Consumer의 실패가
    // 나머지 Consumer의 이벤트 수신을 막지 않도록 개별 격리한다
    public void handle(String streamKey, BaseEvent event) {
        countConsumed(streamKey);
        recordLatency(streamKey, event);

        final List<Consumer<BaseEvent>> consumers = findConsumers(event.getClass());
        if (consumers.isEmpty()) {
            log.warn("등록된 Consumer 없음, 이벤트를 건너뜁니다: eventType={}", EventTypeName.of(event));
            return;
        }
        for (Consumer<BaseEvent> consumer : consumers) {
            try {
                consumer.accept(event);
            } catch (Exception e) {
                log.error(
                        "이벤트 처리 실패: consumer={}, message={}",
                        consumer.getClass().getSimpleName(),
                        event,
                        e);
            }
        }
    }

    // 폴링이 살아 있다는 신호다. Consumer 처리 성공 여부와 무관하게 수신 시점에 센다.
    // 지연 메트릭은 timestamp가 없거나 시각이 뒤집힌 이벤트를 건너뛰므로 소비율의 근거가 못 된다.
    private void countConsumed(String streamKey) {
        try {
            consumedCounters
                    .computeIfAbsent(streamKey, this::registerConsumedCounter)
                    .increment();
        } catch (Exception e) {
            log.warn("Redis Stream 소비 카운터 기록 실패: stream={}", streamKey, e);
        }
    }

    private Counter registerConsumedCounter(String streamKey) {
        return Counter.builder("redis.stream.consumed")
                .description("Redis Stream에서 소비한 메시지 수")
                .tag("stream", streamKey)
                .register(meterRegistry);
    }

    private void recordLatency(String streamKey, BaseEvent event) {
        try {
            latencyMetricService.recordLatency(streamKey, event);
        } catch (Exception e) {
            log.warn("Redis Stream 지연 메트릭 기록 실패: eventId={}", event.eventId(), e);
        }
    }

    private List<Consumer<BaseEvent>> findConsumers(Class<? extends BaseEvent> eventType) {
        final ResolvableType type = ResolvableType.forClassWithGenerics(Consumer.class, eventType);
        final ObjectProvider<Consumer<BaseEvent>> provider = applicationContext.getBeanProvider(type);
        return provider.orderedStream().toList();
    }
}
