package coffeeshout.minigame.racinggame.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.Runner;
import coffeeshout.racinggame.domain.SpeedCalculator;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

class RunnerTest {

    @Test
    void 러너의_초기_속도는_0이다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스());

        // when && then
        assertThat(runner.getSpeed()).isEqualTo(RacingGame.INITIAL_SPEED);
    }

    @Test
    void 속도를_업데이트할_수_있다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스());
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
        final Runner runner = new Runner(PlayerFixture.게스트한스());
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
        final Runner runner = new Runner(PlayerFixture.게스트한스());
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
        final Runner runner = new Runner(PlayerFixture.게스트한스());
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
        final Runner runner = new Runner(PlayerFixture.게스트한스());
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
        final Runner runner = new Runner(PlayerFixture.게스트한스());

        // when && then
        assertThat(runner.getPosition()).isEqualTo(RacingGame.START_LINE);
    }

    @Test
    void 러너는_현재_속도만큼_이동할_수_있다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스());
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
        final Runner runner = new Runner(PlayerFixture.게스트한스());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> 10;
        final Instant now = Instant.now();
        runner.updateSpeed(10, speedCalculator, now);

        // when
        runner.move(now);
        runner.move(now);
        runner.move(now);

        // then
        assertThat(runner.getPosition()).isEqualTo(30);
    }

    @Test
    void 속도가_0이면_이동하지_않는다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스());

        // when
        runner.move(Instant.now());

        // then
        assertThat(runner.getPosition()).isEqualTo(RacingGame.START_LINE);
    }

    @Test
    void 러너가_완주_라인을_넘으면_속도가_0이_된다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> RacingGame.MAX_SPEED;
        final Instant now = Instant.now();
        runner.updateSpeed(10, speedCalculator, now);

        // when
        for (int i = 0; i < 100; i++) {
            runner.move(now);
        }

        // then
        assertThat(runner.getSpeed()).isEqualTo(RacingGame.INITIAL_SPEED);
    }

    @Test
    void 러너가_완주하면_더이상_움직이지_않는다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> RacingGame.MAX_SPEED;
        final Instant now = Instant.now();
        runner.updateSpeed(10, speedCalculator, now);
        for (int i = 0; i < 100; i++) {
            runner.move(now);
        }
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
        final Runner runner = new Runner(PlayerFixture.게스트한스());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> RacingGame.MAX_SPEED;
        final Instant now = Instant.now();
        runner.updateSpeed(10, speedCalculator, now);

        // when
        for (int i = 0; i < 100; i++) {
            runner.move(now);
        }

        // then
        assertThat(runner.isFinished()).isTrue();
    }

    @Test
    void 러너가_완주하지_않았는지_확인할_수_있다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스());

        // when && then
        assertThat(runner.isFinished()).isFalse();
    }

    @Test
    void 러너가_완주하면_완주_시간이_기록된다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> RacingGame.MAX_SPEED;
        final Instant now = Instant.now();
        runner.updateSpeed(10, speedCalculator, now);
        for (int i = 0; i < 99; i++) {
            runner.move(now);
        }

        // when
        runner.move(now);

        // then
        assertThat(runner.getFinishTime()).isNotNull();
        assertThat(runner.isFinished()).isTrue();
    }

    @Test
    void 첫_이동_속도는_최소_속도가_된다() {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스());

        // when
        runner.initializeSpeed();

        // then
        assertThat(runner.getSpeed()).isEqualTo(RacingGame.MIN_SPEED);
    }

    @ParameterizedTest
    @CsvSource({
            "2950, 60, 83",     // 2950 + 60 = 3010, 10 초과
            "2980, 30, 66",     // 2980 + 30 = 3010, 10 초과
            "2990, 20, 50",     // 2990 + 20 = 3010, 10 초과
            "2995, 10, 50",      // 2995 + 10 = 3005, 5 초과
            "2997, 5, 60",       // 2997 + 5 = 3002, 2 초과
            "2999, 3, 33",      // 2999 + 3 = 3002, 2 초과
    })
    void 결승선을_초과하여_도착하면_남은_거리_비율만큼_시간이_보정된다(
            int startPosition,
            int speed,
            int expectedTicksToFinish
    ) {
        // given
        final Runner runner = new Runner(PlayerFixture.게스트한스());
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> speed;
        final Instant tickStartTime = Instant.parse("2025-01-01T00:00:00Z");

        ReflectionTestUtils.setField(runner, "position", startPosition);

        runner.updateSpeed(1, speedCalculator, tickStartTime);

        // when
        runner.move(tickStartTime);
        final Instant expectFinishTime = tickStartTime.minusMillis(RacingGame.MOVE_INTERVAL_MILLIS)
                .plusMillis(expectedTicksToFinish);

        // then
        assertThat(runner.isFinished()).isTrue();
        assertThat(runner.getFinishTime()).isEqualTo(expectFinishTime);
    }
}
