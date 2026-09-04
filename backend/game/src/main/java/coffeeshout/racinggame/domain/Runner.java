package coffeeshout.racinggame.domain;

import static org.springframework.util.Assert.isTrue;

import coffeeshout.gamecommon.Gamer;
import java.time.Instant;
import lombok.Getter;

/**
 * 컨슈머 스레드가 {@link #updateSpeed}를, 스케줄러 스레드가 {@link #move}를 부른다.
 * 가변 필드 넷을 같은 모니터로 잠가 한쪽이 쓰는 중에 다른 쪽이 읽거나 끼어들지 못하게 한다.
 */
public class Runner {

    public static final int SLOW_DOWN_STEP = 3;

    @Getter
    private final Gamer gamer;

    private int position = 0;
    private int speed = RacingGame.INITIAL_SPEED;
    private Instant lastSpeedUpdateTime;
    private Instant finishTime;

    public Runner(Gamer gamer) {
        this.gamer = gamer;
        this.lastSpeedUpdateTime = Instant.now();
    }

    public synchronized void updateSpeed(int tapCount, SpeedCalculator speedCalculator, Instant now) {
        if (isFinished()) {
            return;
        }
        final int nextSpeed = speedCalculator.calculateSpeed(lastSpeedUpdateTime, now, tapCount);
        isTrue(
                nextSpeed >= RacingGame.MIN_SPEED && nextSpeed <= RacingGame.MAX_SPEED,
                String.format("스피드는 0 ~ %d이어야 합니다.", RacingGame.MAX_SPEED));
        this.lastSpeedUpdateTime = now;
        this.speed = nextSpeed;
    }

    public synchronized void move(Instant now) {
        if (isStopped()) {
            return;
        }
        final int nextPosition = position + speed;
        if (crossesFinishLine(nextPosition)) {
            final long remainingMillis =
                    (long) (calculateDistanceToFinishLine(nextPosition) * calculateMillisPerPosition());
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

    public synchronized boolean isFinished() {
        return position >= RacingGame.FINISH_LINE;
    }

    public synchronized void initializeSpeed() {
        this.speed = RacingGame.MIN_SPEED;
    }

    public synchronized void initializeLastSpeedUpdateTime(Instant time) {
        this.lastSpeedUpdateTime = time;
    }

    public synchronized boolean isStopped() {
        return speed == 0;
    }

    public synchronized int getPosition() {
        return position;
    }

    public synchronized int getSpeed() {
        return speed;
    }

    public synchronized Instant getLastSpeedUpdateTime() {
        return lastSpeedUpdateTime;
    }

    public synchronized Instant getFinishTime() {
        return finishTime;
    }
}
