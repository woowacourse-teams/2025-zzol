package coffeeshout.cardgame.config;

import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "card-game.timing")
public record CardGameTimingProperties(
        @Positive(message = "총 라운드 수는 양수여야 합니다") int totalRounds,
        @Positive(message = "첫 번째 로딩 시간은 양수여야 합니다") long firstLoadingMs,
        @Positive(message = "로딩 시간은 양수여야 합니다") long loadingMs,
        @Positive(message = "설명 시간은 양수여야 합니다") long prepareMs,
        @Positive(message = "플레이 시간은 양수여야 합니다") long playingMs,
        @Positive(message = "스코어보드 시간은 양수여야 합니다") long scoreBoardMs,
        @Positive(message = "조기 종료 딜레이는 양수여야 합니다") long earlyFinishDelayMs
) {

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
