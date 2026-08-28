package coffeeshout.racinggame.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.fixture.PlayerFixture;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

class RunnerTest {

    @Test
    void 러너의_초기_속도는_0이다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());

        // when && then
        assertThat(runner.getSpeed()).isEqualTo(RacingGame.INITIAL_SPEED);
    }

    @Test
    void 속도를_업데이트할_수_있다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> 15;
        final Instant now = Instant.now();

        // when
        runner.updateSpeed(10, speedCalculator, now);

        // then
        assertThat(runner.getSpeed()).isEqualTo(15);
    }

    @Test
    void 속도가_최소값보다_작으면_예외가_발생한다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> 2;
        final Instant now = Instant.now();

        // when && then
        assertThatThrownBy(() -> runner.updateSpeed(10, speedCalculator, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("스피드는 0 ~ 60이어야 합니다.");
    }

    @Test
    void 속도가_최대값보다_크면_예외가_발생한다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> 61;
        final Instant now = Instant.now();

        // when && then
        assertThatThrownBy(() -> runner.updateSpeed(10, speedCalculator, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("스피드는 0 ~ 60이어야 합니다.");
    }

    @Test
    void 최소_속도로_업데이트할_수_있다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> RacingGame.MIN_SPEED;
        final Instant now = Instant.now();

        // when
        runner.updateSpeed(10, speedCalculator, now);

        // then
        assertThat(runner.getSpeed()).isEqualTo(RacingGame.MIN_SPEED);
    }

    @Test
    void 최대_속도로_업데이트할_수_있다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> RacingGame.MAX_SPEED;
        final Instant now = Instant.now();

        // when
        runner.updateSpeed(10, speedCalculator, now);

        // then
        assertThat(runner.getSpeed()).isEqualTo(RacingGame.MAX_SPEED);
    }

    @Test
    void 러너의_초기_위치는_0이다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());

        // when && then
        assertThat(runner.getPosition()).isEqualTo(RacingGame.START_LINE);
    }

    @Test
    void 러너는_현재_속도만큼_이동할_수_있다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> 15;
        final Instant now = Instant.now();
        runner.updateSpeed(10, speedCalculator, now);

        // when
        runner.move(now);

        // then
        assertThat(runner.getPosition()).isEqualTo(15);
    }

    @Test
    void 러너는_여러_번_이동할_수_있다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> 10;
        final Instant now = Instant.now();
        runner.updateSpeed(10, speedCalculator, now);

        // when
        달린다(runner, speedCalculator, now, 3);

        // then
        assertThat(runner.getPosition()).isEqualTo(30);
    }

    @Test
    void 속도가_0이면_이동하지_않는다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());

        // when
        runner.move(Instant.now());

        // then
        assertThat(runner.getPosition()).isEqualTo(RacingGame.START_LINE);
    }

    @Test
    void 러너가_완주_라인을_넘으면_속도가_0이_된다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> RacingGame.MAX_SPEED;
        final Instant now = Instant.now();
        runner.updateSpeed(10, speedCalculator, now);

        // when — 완주(50틱) 후에도 계속 돌려 완주 후 감속이 0까지 떨어지는지 본다
        달린다(runner, speedCalculator, now, 100);

        // then
        assertThat(runner.getSpeed()).isEqualTo(RacingGame.INITIAL_SPEED);
    }

    @Test
    void 러너가_완주하면_더이상_움직이지_않는다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> RacingGame.MAX_SPEED;
        final Instant now = Instant.now();
        runner.updateSpeed(10, speedCalculator, now);
        달린다(runner, speedCalculator, now, 100);
        final int finishPosition = runner.getPosition();

        // when
        runner.move(now);
        runner.move(now);

        // then
        assertThat(runner.getPosition()).isEqualTo(finishPosition);
    }

    @Test
    void 러너가_완주했는지_확인할_수_있다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> RacingGame.MAX_SPEED;
        final Instant now = Instant.now();
        runner.updateSpeed(10, speedCalculator, now);

        // when
        달린다(runner, speedCalculator, now, 100);

        // then
        assertThat(runner.isFinished()).isTrue();
    }

    @Test
    void 러너가_완주하지_않았는지_확인할_수_있다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());

        // when && then
        assertThat(runner.isFinished()).isFalse();
    }

    @Test
    void 러너가_완주하면_완주_시간이_기록된다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> RacingGame.MAX_SPEED;
        final Instant now = Instant.now();
        달린다(runner, speedCalculator, now, 49); // 최고 속도 60 × 49틱 = 2940, 아직 결승선 앞이다

        // when
        달린다(runner, speedCalculator, now, 1);

        // then
        assertSoftly(softly -> {
            softly.assertThat(runner.getFinishTime()).isNotNull();
            softly.assertThat(runner.isFinished()).isTrue();
        });
    }

    @Test
    void 탭이_없으면_이동할_때마다_속도가_줄어든다() {
        // given
        final Runner runner = 최고_속도로_달리는_러너();

        // when — 탭 없이 이동만 한다
        runner.move(Instant.now());

        // then
        assertThat(runner.getSpeed()).isEqualTo((int) (RacingGame.MAX_SPEED * RacingGame.SPEED_DECAY_RATE));
    }

    @Test
    void 탭이_없어도_속도는_최저_속도_아래로_떨어지지_않는다() {
        // given — 0이 되면 isStopped()로 영영 못 움직여 완주하지 못한 채 경주가 끝난다
        final Runner runner = 최고_속도로_달리는_러너();
        final Instant now = Instant.now();

        // when
        for (int i = 0; i < 200; i++) {
            runner.move(now);
        }

        // then
        assertSoftly(softly -> {
            softly.assertThat(runner.getSpeed()).isEqualTo(RacingGame.MIN_SPEED);
            softly.assertThat(runner.isStopped()).isFalse();
        });
    }

    @Test
    void 감속된_뒤_다시_탭하면_속도가_회복된다() {
        // given
        final Runner runner = 최고_속도로_달리는_러너();
        final Instant now = Instant.now();
        for (int i = 0; i < 5; i++) {
            runner.move(now);
        }

        // when
        runner.updateSpeed(10, (lastTapedTime, at, tapCount) -> RacingGame.MAX_SPEED, now);

        // then
        assertThat(runner.getSpeed()).isEqualTo(RacingGame.MAX_SPEED);
    }

    @Test
    void 결승선을_넘는_틱에는_아직_주행_중_감속이_적용된다() {
        // given — isSlowingDown()은 position 대입 전에 평가되므로 그 틱은 아직 완주로 보이지 않는다
        final Runner runner = 최고_속도로_달리는_러너();
        ReflectionTestUtils.setField(runner, "position", RacingGame.FINISH_LINE - RacingGame.MAX_SPEED);

        // when
        runner.move(Instant.now());

        // then — SLOW_DOWN_STEP(57)이 아니라 비율 감속(54)이다
        assertSoftly(softly -> {
            softly.assertThat(runner.isFinished()).isTrue();
            softly.assertThat(runner.getSpeed()).isEqualTo((int) (RacingGame.MAX_SPEED * RacingGame.SPEED_DECAY_RATE));
        });
    }

    @Test
    void 완주한_뒤에는_주행_중_감속이_아니라_완주_후_감속이_적용된다() {
        // given — 완주 틱까지 진행시킨다
        final Runner runner = 최고_속도로_달리는_러너();
        final Instant now = Instant.now();
        ReflectionTestUtils.setField(runner, "position", RacingGame.FINISH_LINE - RacingGame.MAX_SPEED);
        runner.move(now);
        final int 완주_틱_속도 = runner.getSpeed();

        // when
        runner.move(now);

        // then — 비율 감속이면 48, 완주 후 감속이면 51이다
        assertThat(runner.getSpeed()).isEqualTo(완주_틱_속도 - Runner.SLOW_DOWN_STEP);
    }

    @Test
    void 첫_이동_속도는_최소_속도가_된다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());

        // when
        runner.initializeSpeed();

        // then
        assertThat(runner.getSpeed()).isEqualTo(RacingGame.MIN_SPEED);
    }

    @ParameterizedTest
    @CsvSource({
        "2950, 60, 83", // 2950 + 60 = 3010, 10 초과
        "2980, 30, 66", // 2980 + 30 = 3010, 10 초과
        "2990, 20, 50", // 2990 + 20 = 3010, 10 초과
        "2995, 10, 50", // 2995 + 10 = 3005, 5 초과
        "2997, 5, 60", // 2997 + 5 = 3002, 2 초과
        "2999, 3, 33", // 2999 + 3 = 3002, 2 초과
    })
    void 결승선을_초과하여_도착하면_남은_거리_비율만큼_시간이_보정된다(int startPosition, int speed, int expectedTicksToFinish) {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> speed;
        final Instant tickStartTime = Instant.parse("2025-01-01T00:00:00Z");

        ReflectionTestUtils.setField(runner, "position", startPosition);

        runner.updateSpeed(1, speedCalculator, tickStartTime);

        // when
        runner.move(tickStartTime);
        final Instant expectFinishTime =
                tickStartTime.minusMillis(RacingGame.MOVE_INTERVAL_MILLIS).plusMillis(expectedTicksToFinish);

        // then
        assertThat(runner.isFinished()).isTrue();
        assertThat(runner.getFinishTime()).isEqualTo(expectFinishTime);
    }

    private Runner 최고_속도로_달리는_러너() {
        final Runner runner = new Runner(PlayerFixture.게스트한스().toGamer());
        runner.updateSpeed(10, (lastTapedTime, at, tapCount) -> RacingGame.MAX_SPEED, Instant.now());
        return runner;
    }

    /**
     * 매 틱 탭하며 달린다. 주행 중 감속이 있으므로 속도를 한 번만 주고 반복 이동하면
     * 속도가 계속 줄어 완주하지 못한다 — 실제 플레이(계속 누르기)와 같은 조건을 만든다.
     */
    private void 달린다(Runner runner, SpeedCalculator speedCalculator, Instant now, int ticks) {
        for (int i = 0; i < ticks; i++) {
            runner.updateSpeed(10, speedCalculator, now);
            runner.move(now);
        }
    }
}
