package coffeeshout.racinggame.domain;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class TapPerSecondSpeedCalculator implements SpeedCalculator {

    @Override
    public int calculateSpeed(Instant lastTapedTime, Instant now, int tapCount) {
        final Duration duration = Duration.between(lastTapedTime, now);
        return convertToSpeed(calculateClickPerSecond(tapCount, duration));
    }

    private double calculateClickPerSecond(int boundedClickCount, Duration duration) {
        final long millis = Math.max(1, duration.toMillis());
        return (double) boundedClickCount / millis * 1000;
    }

    private int convertToSpeed(double clicksPerSecond) {
        double speed = clicksPerSecond * RacingGame.CLICK_PER_SPEED_SCALE;
        return Math.clamp((int) speed, RacingGame.MIN_SPEED, RacingGame.MAX_SPEED);
    }
}
