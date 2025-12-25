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
            final ResolvableType type = beanFactory.getMergedBeanDefinition(beanName).getResolvableType();
            final ResolvableType consumerType = type.as(Consumer.class);
            final Class<?> eventType = consumerType.getGeneric(0).resolve();

            if (eventType == null) {
                continue;
            }

            redisObjectMapper.registerSubtypes(eventType);
            log.debug("Registered event subtype: {} from bean: {}", eventType.getSimpleName(), beanName);
        }
    }
}
