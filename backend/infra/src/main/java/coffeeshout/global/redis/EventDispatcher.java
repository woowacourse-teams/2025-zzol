package coffeeshout.global.redis;

import coffeeshout.global.metric.RedisStreamLatencyMetricService;
import java.util.List;
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

    // 동일 이벤트 타입의 Consumer 전체에 팬아웃한다 (ADR-0023 결정 6 — 예: RoomCreateEvent를
    // RoomCreateConsumer와 GameSessionInitConsumer가 함께 처리). 한 Consumer의 실패가
    // 나머지 Consumer의 이벤트 수신을 막지 않도록 개별 격리한다
    public void handle(BaseEvent event) {
        recordLatency(event);

        final List<Consumer<BaseEvent>> consumers = findConsumers(event.getClass());
        if (consumers.isEmpty()) {
            log.warn("등록된 Consumer 없음, 이벤트를 건너뜁니다: eventType={}", event.getClass().getSimpleName());
            return;
        }
        for (Consumer<BaseEvent> consumer : consumers) {
            try {
                consumer.accept(event);
            } catch (Exception e) {
                log.error("이벤트 처리 실패: consumer={}, message={}", consumer.getClass().getSimpleName(), event, e);
            }
        }
    }

    private void recordLatency(BaseEvent event) {
        try {
            latencyMetricService.recordLatency(event);
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
