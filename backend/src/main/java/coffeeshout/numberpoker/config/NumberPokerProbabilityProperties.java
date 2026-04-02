package coffeeshout.numberpoker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@Validated
@ConfigurationProperties(prefix = "number-poker.probability")
public record NumberPokerProbabilityProperties(
        @DecimalMin(value = "0.0", inclusive = true)
        @DecimalMax(value = "1.0", inclusive = true)
        double stage1FoldMultiplier,
        @DecimalMin(value = "0.0", inclusive = true)
        @DecimalMax(value = "1.0", inclusive = true)
        double stage2FoldMultiplier
) {

    public NumberPokerProbabilityProperties {
        if (stage1FoldMultiplier >= stage2FoldMultiplier) {
            throw new IllegalArgumentException(
                    "stage1FoldMultiplier must be less than stage2FoldMultiplier"
            );
        }
    }
}
