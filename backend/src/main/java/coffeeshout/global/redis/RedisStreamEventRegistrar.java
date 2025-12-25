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
        inspectConsumerBeans();
    }

    public void inspectConsumerBeans() {
        final String[] beanNames = beanFactory.getBeanNamesForType(Consumer.class);
        for (String beanName : beanNames) {
            try {
                final ResolvableType type = beanFactory.getMergedBeanDefinition(beanName).getResolvableType();
                if (type == ResolvableType.NONE) {
                    log.warn("Cannot resolve type for bean: {}", beanName);
                    continue;
                }

                final ResolvableType consumerType = type.as(Consumer.class);
                if (consumerType == ResolvableType.NONE) {
                    log.warn("Bean {} does not properly implement Consumer interface", beanName);
                    continue;
                }

                final ResolvableType genericType = consumerType.getGeneric(0);
                if (genericType == ResolvableType.NONE) {
                    log.warn("Bean {} has raw type Consumer without generic parameter", beanName);
                    continue;
                }

                final Class<?> eventType = genericType.resolve();
                if (eventType == null) {
                    log.warn("Cannot resolve generic type for bean: {}", beanName);
                    continue;
                }

                if (!BaseEvent.class.isAssignableFrom(eventType)) {
                    log.debug("Skipping bean {} - generic type {} is not a BaseEvent subtype",
                            beanName, eventType.getSimpleName());
                    continue;
                }

                redisObjectMapper.registerSubtypes(eventType);
                log.debug("Registered event subtype: {} from bean: {}", eventType.getSimpleName(), beanName);
            } catch (Exception e) {
                log.warn("Failed to process Consumer bean: {} - {}", beanName, e.getMessage(), e);
            }
        }
    }
}
