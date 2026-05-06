package coffeeshout.global.zzolbot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "zzol-bot")
public record ZzolBotProperties(
        String geminiApiKey,
        @NotBlank String model,
        @Positive int maxLoopIterations,
        @NotNull @Valid MonitoringProperties monitoring,
        @NotNull @Valid DeterminismProperties determinism,
        @Positive int defaultWindowMinutes,
        @Positive long toolTimeoutMillis
) {

    public record MonitoringProperties(
            @NotBlank String lokiUrl,
            @NotBlank String tempoUrl,
            @NotBlank String prometheusUrl,
            @NotBlank String environment
    ) {}

    public record DeterminismProperties(
            @DecimalMin("0.0") @DecimalMax("2.0") double temperature,
            @DecimalMin("0.0") @DecimalMax("1.0") double topP
    ) {}
}
