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
            if (beanFactory.getBeanDefinition(beanName) instanceof RootBeanDefinition) {
                final ResolvableType type = beanFactory.getMergedBeanDefinition(beanName).getResolvableType();
                final Class<?>[] classes = type.resolveGenerics();
                if (classes.length == 0) {
                    continue;
                }
                redisObjectMapper.registerSubtypes(classes[0]);
            }
        }
    }
}
