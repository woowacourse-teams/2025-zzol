package coffeeshout.cardgame.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "card-game.timing")
public record CardGameTimingProperties(
        @NotNull Duration firstLoading,
        @NotNull Duration loading,
        @NotNull Duration prepare,
        @NotNull Duration playing,
        @NotNull Duration scoreBoard,
        @NotNull Duration earlyFinishDelay
) {
}
