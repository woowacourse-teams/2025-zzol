package coffeeshout.zzolbot.remediation.config;

import coffeeshout.zzolbot.remediation.application.RemediationBudget;
import coffeeshout.zzolbot.remediation.infra.DeniedRemediationBudget;
import coffeeshout.zzolbot.remediation.infra.RedisRemediationBudget;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 자동 수정 봇 빈 배선. 일일 디스패치 예산은 Redis(StringRedisTemplate)가 있으면 분산 카운터를 쓰고,
 * 없으면 fail-closed로 거부한다 — 자동 수정은 실코드 PR을 만들므로 캡을 강제 못 하면 차라리 막는다.
 * {@code LlmBudgetConfig}와 같은 이유로 {@link ObjectProvider}로 늦게 해석한다.
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
            log.error("[ZzolBot] StringRedisTemplate 부재 — 일일 예산을 강제할 수 없어 자동 수정 디스패치를 차단(fail-closed)");
            return new DeniedRemediationBudget();
        }
        return new RedisRemediationBudget(redisTemplate, properties, clock);
    }
}
