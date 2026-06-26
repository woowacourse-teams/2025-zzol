package coffeeshout.zzolbot.remediation.config;

import coffeeshout.zzolbot.remediation.application.RemediationBudget;
import coffeeshout.zzolbot.remediation.infra.RedisRemediationBudget;
import coffeeshout.zzolbot.remediation.infra.UnlimitedRemediationBudget;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 자동 수정 봇 빈 배선. 일일 디스패치 예산은 Redis(StringRedisTemplate) 유무에 따라 분산 카운터/무제한
 * 폴백 중 하나를 고른다. {@code LlmBudgetConfig}와 같은 이유로 {@link ObjectProvider}로 늦게 해석한다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RemediationProperties.class)
public class RemediationConfig {

    @Bean
    public RemediationBudget remediationBudget(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            RemediationProperties properties,
            Clock clock) {
        final StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            log.warn("[ZzolBot] StringRedisTemplate 부재 — 무제한 수정 예산 폴백 사용(단일 인스턴스 가정)");
            return new UnlimitedRemediationBudget();
        }
        return new RedisRemediationBudget(redisTemplate, properties, clock);
    }
}
