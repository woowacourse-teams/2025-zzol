package coffeeshout.global.zzolbot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "zzol-bot")
public record ZzolBotProperties(
        String geminiApiKey,
        @NotBlank String model,
        @Positive int maxLoopIterations,
        @Valid MonitoringProperties monitoring
) {

    public record MonitoringProperties(
            @NotBlank String lokiUrl,
            @NotBlank String tempoUrl,
            @NotBlank String prometheusUrl
    ) {}
}
