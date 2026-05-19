package coffeeshout.report.ratelimit;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.redisson.api.RateIntervalUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "report.rate-limit")
public record ReportRateLimitProperties(
        @Positive long rate,
        @Positive long rateInterval,
        @NotNull RateIntervalUnit rateIntervalUnit,
        @NotNull Duration ttl
) {
}
