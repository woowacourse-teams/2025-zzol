package coffeeshout.global.redis;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class ConsumerStartupValidator implements SmartInitializingSingleton {

    private static final String BASE_PACKAGE = "coffeeshout";

    private final ApplicationContext applicationContext;

    @Override
    public void afterSingletonsInstantiated() {
        final Reflections reflections = new Reflections(BASE_PACKAGE);
        final Set<Class<? extends BaseEvent>> eventTypes = reflections.getSubTypesOf(BaseEvent.class);

        final List<String> missing = new ArrayList<>();
        for (Class<? extends BaseEvent> eventType : eventTypes) {
            if (isAbstractOrInterface(eventType)) {
                continue;
            }
            final ResolvableType type = ResolvableType.forClassWithGenerics(Consumer.class, eventType);
            // 한 이벤트 타입에 Consumer가 여럿일 수 있다(EventDispatcher 팬아웃 — 예: GameRoomCreatedEvent를
            // RoomCreateConsumer + GameSessionInitConsumer가 함께 처리). getIfAvailable()은 다중 후보에서
            // NoUniqueBeanDefinitionException을 던지므로, 존재 여부만 stream()으로 확인한다 (ADR-0025 결정 6)
            if (applicationContext.getBeanProvider(type).stream().findAny().isEmpty()) {
                missing.add(eventType.getName());
            }
        }

        if (missing.isEmpty()) {
            log.info("모든 BaseEvent 타입에 Consumer가 등록되어 있습니다 ({}개)", eventTypes.size());
            return;
        }
        missing.forEach(eventType -> log.error("Consumer 없음: {}", eventType));
        throw new IllegalStateException(
                "Consumer가 등록되지 않은 이벤트 타입 " + missing.size() + "개 — 위 로그를 확인하세요"
        );
    }

    private boolean isAbstractOrInterface(Class<?> clazz) {
        return clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers());
    }
}
