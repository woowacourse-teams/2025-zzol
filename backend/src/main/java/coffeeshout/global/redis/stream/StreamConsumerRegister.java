package coffeeshout.global.redis.stream;

import coffeeshout.global.redis.BaseEvent;
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
public class StreamConsumerRegister {

    private final ApplicationContext applicationContext;

    public <T extends BaseEvent> Consumer<T> getConsumer(Class<T> eventType) {
        final ResolvableType type = ResolvableType.forClassWithGenerics(Consumer.class, eventType);
        final ObjectProvider<Consumer<T>> provider = applicationContext.getBeanProvider(type);
        return provider.getObject();
    }
}
