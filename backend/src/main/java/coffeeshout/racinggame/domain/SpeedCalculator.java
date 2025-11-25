package coffeeshout.racinggame.domain;

import java.time.Instant;

public interface SpeedCalculator {

    int calculateSpeed(Instant lastTapedTime, Instant now, int tapCount);
}
