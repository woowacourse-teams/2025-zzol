package coffeeshout.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

@Slf4j
@Configuration
public class RedisStreamEventRegistrar implements SmartInitializingSingleton {

    private final ObjectMapper redisObjectMapper;
    private final ConfigurableListableBeanFactory beanFactory;

    public RedisStreamEventRegistrar(
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper,
            ConfigurableListableBeanFactory beanFactory
    ) {
        this.redisObjectMapper = redisObjectMapper;
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        final String[] beanNames = beanFactory.getBeanNamesForType(Consumer.class);
        for (String beanName : beanNames) {
            try {
                registerEventTypeFromConsumer(beanName);
            } catch (Exception e) {
                log.warn("Failed to process Consumer bean: {} - {}", beanName, e.getMessage(), e);
            }
        }
    }

    private void registerEventTypeFromConsumer(String beanName) {
        final Class<?> eventType = resolveEventType(beanName);
        if (eventType == null) {
            log.warn("Bean {} does not implement Consumer<T> with resolvable generic type", beanName);
            return;
        }

        if (!BaseEvent.class.isAssignableFrom(eventType)) {
            log.debug("Skipping bean {} - generic type {} is not a BaseEvent subtype",
                    beanName, eventType.getSimpleName());
            return;
        }

        redisObjectMapper.registerSubtypes(eventType);
        log.debug("Registered event subtype: {} from bean: {}", eventType.getSimpleName(), beanName);
    }

    private Class<?> resolveEventType(String beanName) {
        final ResolvableType type = beanFactory.getMergedBeanDefinition(beanName).getResolvableType();

        return type.as(Consumer.class).getGeneric(0).resolve();
    }
}
