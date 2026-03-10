package coffeeshout.speedtouch.domain;

import lombok.Getter;

@Getter
public enum SpeedTouchGameState {

    DESCRIPTION(4000L),
    PREPARE(2000L),
    PLAYING(0L),
    DONE(0L),
    ;

    private final long duration;

    SpeedTouchGameState(long duration) {
        this.duration = duration;
    }
}
