package coffeeshout.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
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
            final Class<?> beanType = beanFactory.getType(beanName);
            if (beanType == null) {
                continue;
            }

            final ResolvableType resolvableType = ResolvableType.forClass(beanType).as(Consumer.class);
            final Class<?> eventType = resolvableType.getGeneric(0).resolve();

            if (eventType != null && BaseEvent.class.isAssignableFrom(eventType)) {
                redisObjectMapper.registerSubtypes(eventType);
                log.debug("Registered event subtype: {} from bean: {}", eventType.getSimpleName(), beanName);
            }
        }
    }
}
