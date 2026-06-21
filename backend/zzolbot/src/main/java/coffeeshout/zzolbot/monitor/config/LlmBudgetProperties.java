package coffeeshout.zzolbot.monitor.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 일일 LLM 호출 예산 설정.
 */
@Validated
@ConfigurationProperties(prefix = "zzol-bot.llm-budget")
public record LlmBudgetProperties(
        @Positive long dailyMax
) {
}
