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

    /**
     * 탭(WS 인바운드 스레드)과 자동 이동(스케줄러 스레드)이 같은 {@code speed}를 쓴다.
     * 주행 중 감속이 생기기 전에는 두 스레드의 쓰기 구간이 겹치지 않았지만
     * ({@code slowDown()}은 완주 후에만 돌고 그때 updateSpeed는 조기 반환한다),
     * {@code decay()}가 경주 내내 돌면서 겹치게 됐다. 러너 단위 락으로 막는다 —
     * 틱은 초당 10회, 탭은 초당 5회라 경합 비용은 무시할 수 있다.
     */
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

    /** 락 이유는 {@link #updateSpeed} 주석 참고. 한 틱 안에서 speed를 여러 번 읽으므로 값이 흔들리면 안 된다. */
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
        } else {
            decay();
        }
        this.position = nextPosition;
    }

    /**
     * 주행 중 감속. 무엇을 막는 값인지는 {@link RacingGame#SPEED_DECAY_RATE} 주석 참고.
     *
     * <p>결승선을 넘는 그 틱에도 한 번 걸린다 — {@code isSlowingDown()}이 position 대입 전에
     * 평가되므로 아직 완주로 보이지 않는다. finishTime은 그 전에 확정되므로 순위·기록에는
     * 영향이 없고, 완주 후 관성이 한 틱 짧아질 뿐이다.
     *
     * <p>MIN_SPEED가 하한인 이유는 속도가 0이 되면 {@code isStopped()}로 영영 못 움직여
     * 완주하지 못한 채 경주가 끝나기 때문이다. 그러면 finishTime이 null로 남아
     * {@code RacingGame#convertScore}의 {@code Duration.between(startTime, null)}이 터진다.
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
