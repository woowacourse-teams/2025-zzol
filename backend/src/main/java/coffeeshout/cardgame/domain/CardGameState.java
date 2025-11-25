package coffeeshout.cardgame.domain;

import java.time.Duration;
import lombok.Getter;

@Getter
public enum CardGameState {
    READY(0),
    FIRST_LOADING(4000),
    LOADING(3000),
    PREPARE(2000),
    PLAYING(10250),
    SCORE_BOARD(1500),
    DONE(0),
    ;

    private final int duration;

    CardGameState(int duration) {
        this.duration = duration;
    }

    public Duration getDurationMillis() {
        return Duration.ofMillis(duration);
    }
}
