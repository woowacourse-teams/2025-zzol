package coffeeshout.racinggame.domain;

import static org.springframework.util.Assert.isTrue;

import coffeeshout.gamecommon.Gamer;
import java.time.Instant;
import lombok.Getter;

@Getter
public class Runner {

    public static final int SLOW_DOWN_STEP = 3;
    private final Gamer gamer;

    private int position = 0;
    private int speed = RacingGame.INITIAL_SPEED;
    private Instant lastSpeedUpdateTime;
    private Instant finishTime;

    public Runner(Gamer gamer) {
        this.gamer = gamer;
        this.lastSpeedUpdateTime = Instant.now();
    }

    public void updateSpeed(int tapCount, SpeedCalculator speedCalculator, Instant now) {
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

    public void move(Instant now) {
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
        } else {
            decay();
        }
        this.position = nextPosition;
    }

    /**
     * 주행 중 감속. 속도 갱신이 탭에서만 일어나면 최고 속도를 찍고 손을 떼도 그 속도가 굳어
     * 조작 없이 완주한다. 매 틱 줄여 두고 탭이 다시 올려주게 해야 계속 누르는 쪽이 빠르다.
     * MIN_SPEED가 하한인 이유는 속도가 0이 되면 isStopped()로 영영 못 움직여
     * 완주하지 못한 채 경주가 끝나기 때문이다(finishTime이 null로 남는다).
     */
    private void decay() {
        this.speed = Math.max(RacingGame.MIN_SPEED, (int) (speed * RacingGame.SPEED_DECAY_RATE));
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
