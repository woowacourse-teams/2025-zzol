package coffeeshout.numberpoker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "number-poker.probability")
public record NumberPokerProbabilityProperties(
        double stage1FoldMultiplier,
        double stage2FoldMultiplier,
        int adjustmentStep
) {
}
