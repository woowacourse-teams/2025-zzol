package coffeeshout.global.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "security.ip-block")
public record IpBlockProperties(
        @DefaultValue("5") int notFoundThreshold,
        @DefaultValue("1h") Duration notFoundWindow,
        @DefaultValue("24h") Duration blockTtl
) {
}
