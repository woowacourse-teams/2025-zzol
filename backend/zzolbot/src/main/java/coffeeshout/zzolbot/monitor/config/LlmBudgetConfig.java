package coffeeshout.zzolbot.monitor.config;

import coffeeshout.zzolbot.monitor.application.LlmCallBudget;
import coffeeshout.zzolbot.monitor.infra.RedisLlmCallBudget;
import coffeeshout.zzolbot.monitor.infra.UnlimitedLlmCallBudget;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 일일 예산 빈 배선. Redis(StringRedisTemplate) 유무에 따라 분산 카운터/무제한 폴백 중 하나를 고른다.
 *
 * <p>{@code StringRedisTemplate}은 Spring Boot RedisAutoConfiguration이 제공하므로, 사용자 설정에서
 * {@code @ConditionalOnBean}으로 판정하면 auto-configuration보다 먼저 평가돼 항상 폴백이 잡히는 함정이 있다.
 * 빈 생성 시점에 해석되는 {@link ObjectProvider}로 가져와 그 타이밍 문제를 피한다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LlmBudgetProperties.class)
public class LlmBudgetConfig {

    @Bean
    public LlmCallBudget llmCallBudget(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            LlmBudgetProperties properties,
            Clock clock) {
        final StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            log.warn("[ZzolBot] StringRedisTemplate 부재 — 무제한 예산 폴백 사용(단일 인스턴스 가정)");
            return new UnlimitedLlmCallBudget();
        }
        return new RedisLlmCallBudget(redisTemplate, properties, clock);
    }
}
