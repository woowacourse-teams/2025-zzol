package coffeeshout.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
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
            final ResolvableType resolvableType = getResolvableTypeFromBeanDefinition(beanName);
            if (resolvableType == null) {
                continue;
            }

            final Class<?> eventType = resolvableType.getGeneric(0).resolve();

            if (eventType != null && BaseEvent.class.isAssignableFrom(eventType)) {
                redisObjectMapper.registerSubtypes(eventType);
                log.debug("Registered event subtype: {} from bean: {}", eventType.getSimpleName(), beanName);
            }
        }
    }

    private ResolvableType getResolvableTypeFromBeanDefinition(String beanName) {
        // 방법 1: BeanDefinition에서 제네릭 타입 추출 (@Bean 방식 - 람다 지원)
        final ResolvableType fromBeanDefinition = extractFromBeanDefinition(beanName);
        if (fromBeanDefinition != null) {
            return fromBeanDefinition;
        }

        // 방법 2: 클래스에서 제네릭 타입 추출 (@Component 방식)
        return extractFromBeanType(beanName);
    }

    private ResolvableType extractFromBeanDefinition(String beanName) {
        if (!beanFactory.containsBeanDefinition(beanName)) {
            return null;
        }

        final BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        if (!(beanDefinition instanceof RootBeanDefinition rootBeanDefinition)) {
            return null;
        }

        final ResolvableType resolvableType = rootBeanDefinition.getResolvableType();
        if (resolvableType == ResolvableType.NONE) {
            return null;
        }

        final ResolvableType consumerType = resolvableType.as(Consumer.class);
        return consumerType != ResolvableType.NONE ? consumerType : null;
    }

    private ResolvableType extractFromBeanType(String beanName) {
        final Class<?> beanType = beanFactory.getType(beanName);
        return beanType != null ? ResolvableType.forClass(beanType).as(Consumer.class) : null;
    }
}
