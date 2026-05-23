package coffeeshout.racinggame.domain;

import lombok.Getter;

@Getter
public enum RacingGameState {
    DESCRIPTION(4000L),
    PREPARE(2000L),
    PLAYING(30000L),
    DONE(0L),
    ;

    final long duration;

    RacingGameState(long duration) {
        this.duration = duration;
    }
}
