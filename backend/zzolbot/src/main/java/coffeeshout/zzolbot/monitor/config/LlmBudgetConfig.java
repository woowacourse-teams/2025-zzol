package coffeeshout.zzolbot.monitor.config;

import coffeeshout.zzolbot.monitor.application.LlmCallBudget;
import coffeeshout.zzolbot.monitor.infra.RedisLlmCallBudget;
import coffeeshout.zzolbot.monitor.infra.UnlimitedLlmCallBudget;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 일일 예산 빈 배선. Redis(StringRedisTemplate) 유무에 따라 분산 카운터/무제한 폴백 중 하나만 등록된다.
 */
@Configuration
@EnableConfigurationProperties(LlmBudgetProperties.class)
public class LlmBudgetConfig {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public LlmCallBudget redisLlmCallBudget(
            StringRedisTemplate redisTemplate, LlmBudgetProperties properties, Clock clock) {
        return new RedisLlmCallBudget(redisTemplate, properties, clock);
    }

    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public LlmCallBudget unlimitedLlmCallBudget() {
        return new UnlimitedLlmCallBudget();
    }
}
