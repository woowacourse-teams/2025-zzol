package coffeeshout.cardgame.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "card-game.timing")
public record CardGameTimingProperties(
        long firstLoadingMs,
        long loadingMs,
        long prepareMs,
        long playingMs,
        long scoreBoardMs,
        long earlyFinishDelayMs
) {

    public CardGameTimingProperties {
        if (firstLoadingMs <= 0) firstLoadingMs = 4000;
        if (loadingMs <= 0) loadingMs = 3000;
        if (prepareMs <= 0) prepareMs = 2000;
        if (playingMs <= 0) playingMs = 10250;
        if (scoreBoardMs <= 0) scoreBoardMs = 1500;
        if (earlyFinishDelayMs <= 0) earlyFinishDelayMs = 2000;
    }

    public Duration firstLoading() {
        return Duration.ofMillis(firstLoadingMs);
    }

    public Duration loading() {
        return Duration.ofMillis(loadingMs);
    }

    public Duration prepare() {
        return Duration.ofMillis(prepareMs);
    }

    public Duration playing() {
        return Duration.ofMillis(playingMs);
    }

    public Duration scoreBoard() {
        return Duration.ofMillis(scoreBoardMs);
    }

    public Duration earlyFinishDelay() {
        return Duration.ofMillis(earlyFinishDelayMs);
    }
}
