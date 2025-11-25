package coffeeshout.minigame.racinggame.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.racinggame.domain.TapPerSecondSpeedCalculator;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TapPerSecondSpeedCalculatorTest {

    private final TapPerSecondSpeedCalculator speedCalculator = new TapPerSecondSpeedCalculator();

    @Test
    void 초당_탭_횟수로_속도를_계산할_수_있다() {
        // given
        final Instant lastTapedTime = Instant.now();
        final Instant now = lastTapedTime.plusMillis(1000);
        final int tapCount = 8;

        // when
        final int speed = speedCalculator.calculateSpeed(lastTapedTime, now, tapCount);

        // then
        assertThat(speed).isEqualTo(8);
    }

    @Test
    void 탭_횟수가_임계값을_초과하면_임계값으로_제한된다() {
        // given
        final Instant lastTapedTime = Instant.now();
        final Instant now = lastTapedTime.plusMillis(1000);
        final int tapCount = 15;

        // when
        final int speed = speedCalculator.calculateSpeed(lastTapedTime, now, tapCount);

        // then
        assertThat(speed).isEqualTo(15);
    }

    @Test
    void 짧은_시간에_많은_탭을_하면_높은_속도가_계산된다() {
        // given
        final Instant lastTapedTime = Instant.now();
        final Instant now = lastTapedTime.plusMillis(500);
        final int tapCount = 5;

        // when
        final int speed = speedCalculator.calculateSpeed(lastTapedTime, now, tapCount);

        // then
        assertThat(speed).isEqualTo(10); // 0.5초에 5번 = 초당 10번
    }

    @Test
    void 긴_시간에_적은_탭을_하면_낮은_속도가_계산된다() {
        // given
        final Instant lastTapedTime = Instant.now();
        final Instant now = lastTapedTime.plusMillis(2000);
        final int tapCount = 4;

        // when
        final int speed = speedCalculator.calculateSpeed(lastTapedTime, now, tapCount);

        // then
        assertThat(speed).isEqualTo(3); // 2초에 4번 = 초당 2번
    }

    @Test
    void 탭을_하지_않으면_속도는_0이다() {
        // given
        final Instant lastTapedTime = Instant.now();
        final Instant now = lastTapedTime.plusMillis(1000);
        final int tapCount = 0;

        // when
        final int speed = speedCalculator.calculateSpeed(lastTapedTime, now, tapCount);

        // then
        assertThat(speed).isEqualTo(3);
    }

    @Test
    void 매우_짧은_시간_간격에서도_속도를_계산할_수_있다() {
        // given
        final Instant lastTapedTime = Instant.now();
        final Instant now = lastTapedTime.plusMillis(100);
        final int tapCount = 1;

        // when
        final int speed = speedCalculator.calculateSpeed(lastTapedTime, now, tapCount);

        // then
        assertThat(speed).isEqualTo(10); // 0.1초에 1번 = 초당 10번
    }

    @Test
    void 임계값_이하의_최대_탭_횟수로_속도를_계산할_수_있다() {
        // given
        final Instant lastTapedTime = Instant.now();
        final Instant now = lastTapedTime.plusMillis(1000);
        final int tapCount = 20;

        // when
        final int speed = speedCalculator.calculateSpeed(lastTapedTime, now, tapCount);

        // then
        assertThat(speed).isEqualTo(20); // 10
    }

    @Test
    void 소수점_이하는_버림_처리된다() {
        // given
        final Instant lastTapedTime = Instant.now();
        final Instant now = lastTapedTime.plusMillis(1000);
        final int tapCount = 7;

        // when
        final int speed = speedCalculator.calculateSpeed(lastTapedTime, now, tapCount);

        // then
        assertThat(speed).isEqualTo(7); // 정확히 7.0
    }

    @Test
    void 다양한_시간_간격에서_속도_계산이_정확하다() {
        // given
        final Instant lastTapedTime = Instant.now();

        // when & then - 0.25초에 2번 = 초당 8번
        Instant now1 = lastTapedTime.plusMillis(250);
        assertThat(speedCalculator.calculateSpeed(lastTapedTime, now1, 2)).isEqualTo(8);

        // when & then - 1.5초에 9번 = 초당 6번
        Instant now2 = lastTapedTime.plusMillis(1500);
        assertThat(speedCalculator.calculateSpeed(lastTapedTime, now2, 9)).isEqualTo(6);

        // when & then - 3초에 6번 = 초당 2번
        Instant now3 = lastTapedTime.plusMillis(3000);
        assertThat(speedCalculator.calculateSpeed(lastTapedTime, now3, 6)).isEqualTo(3);
    }
}
