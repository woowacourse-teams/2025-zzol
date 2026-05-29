package coffeeshout.report.ratelimit;

import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "report.rate-limit")
public record ReportRateLimitProperties(
        @Positive long rate,
        @DurationMin(nanos = 1, message = "레이트리밋 간격은 0보다 커야 합니다") Duration rateInterval,
        @DurationMin(nanos = 1, message = "레이트리밋 TTL은 0보다 커야 합니다") Duration ttl
) {
}
