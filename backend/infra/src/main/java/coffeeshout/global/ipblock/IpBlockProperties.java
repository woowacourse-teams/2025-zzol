package coffeeshout.global.ipblock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import java.util.List;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.ip-block")
public record IpBlockProperties(
        @Min(value = 1, message = "Not Found 누적 임계값은 1 이상이어야 합니다") @DefaultValue("5") int notFoundThreshold,
        @DurationMin(nanos = 1, message = "Not Found 누적 윈도우는 0보다 커야 합니다") @DefaultValue("1h") Duration notFoundWindow,
        @DurationMin(nanos = 1, message = "차단 TTL은 0보다 커야 합니다") @DefaultValue("24h") Duration blockTtl,
        @NotEmpty @DefaultValue({"/admin", "/reports"}) List<String> exemptPaths,
        @DefaultValue("/ws") List<String> notFoundExemptPaths
) {
}
