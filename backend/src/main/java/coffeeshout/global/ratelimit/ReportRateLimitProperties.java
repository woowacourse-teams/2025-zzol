package coffeeshout.global.ratelimit;

import org.redisson.api.RateIntervalUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "report.rate-limit")
public record ReportRateLimitProperties(
        long rate,
        long rateInterval,
        RateIntervalUnit rateIntervalUnit
) {

    public ReportRateLimitProperties {
        if (rate <= 0) rate = 5;
        if (rateInterval <= 0) rateInterval = 1;
        if (rateIntervalUnit == null) rateIntervalUnit = RateIntervalUnit.HOURS;
    }
}
