package coffeeshout.racinggame.domain;

import static org.springframework.util.Assert.isTrue;

import coffeeshout.room.domain.player.Player;
import java.time.Instant;
import lombok.Getter;

@Getter
public class Runner {

    public static final int SLOW_DOWN_STEP = 3;
    private final Player player;

    private int position = 0;
    private int speed = RacingGame.INITIAL_SPEED;
    private Instant lastSpeedUpdateTime;
    private Instant finishTime;

    public Runner(Player player) {
        this.player = player;
        this.lastSpeedUpdateTime = Instant.now();
    }

    public void updateSpeed(int tapCount, SpeedCalculator speedCalculator, Instant now) {
        if (isFinished()) {
            return;
        }
        final int nextSpeed = speedCalculator.calculateSpeed(lastSpeedUpdateTime, now, tapCount);
        isTrue(nextSpeed >= RacingGame.MIN_SPEED && nextSpeed <= RacingGame.MAX_SPEED,
                String.format("스피드는 0 ~ %d이어야 합니다.", RacingGame.MAX_SPEED));
        this.lastSpeedUpdateTime = now;
        this.speed = nextSpeed;
    }

    public void move(Instant now) {
        if (isStopped()) {
            return;
        }
        final int nextPosition = position + speed;
        if (crossesFinishLine(nextPosition)) {
            final long remainingMillis = (long) (calculateDistanceToFinishLine(nextPosition) * calculateMillisPerPosition());
            finishTime = now.minusMillis(RacingGame.MOVE_INTERVAL_MILLIS).plusMillis(remainingMillis);
        }
        if (isSlowingDown()) {
            slowDown();
        }
        this.position = nextPosition;
    }

    private boolean isSlowingDown() {
        return isFinished() && !isStopped();
    }

    private double calculateMillisPerPosition() {
        return RacingGame.MOVE_INTERVAL_MILLIS / (double) speed;
    }

    private int calculateDistanceToFinishLine(int nextPosition) {
        return speed - nextPosition % RacingGame.FINISH_LINE;
    }

    private boolean crossesFinishLine(int nextPosition) {
        return nextPosition >= RacingGame.FINISH_LINE && !isFinished();
    }

    private void slowDown() {
        if (speed - SLOW_DOWN_STEP <= 0) {
            speed = 0;
            return;
        }
        speed -= SLOW_DOWN_STEP;
    }

    public boolean isFinished() {
        return position >= RacingGame.FINISH_LINE;
    }

    public void initializeSpeed() {
        this.speed = RacingGame.MIN_SPEED;
    }

    public void initializeLastSpeedUpdateTime(Instant time) {
        this.lastSpeedUpdateTime = time;
    }

    public boolean isStopped() {
        return speed == 0;
    }
}
