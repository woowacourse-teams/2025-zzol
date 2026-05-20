package coffeeshout.room.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "roulette")
public record RouletteProperties(
        @DecimalMin("0.1") @DecimalMax("0.9") double defaultAdjustmentWeight
) {
}
