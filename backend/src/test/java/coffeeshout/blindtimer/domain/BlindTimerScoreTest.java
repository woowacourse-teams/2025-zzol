package coffeeshout.blindtimer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BlindTimerScoreTest {

    @Nested
    class 정상_STOP_점수 {

        @Test
        void 오차가_작을수록_값이_작다() {
            // given - 목표 10초, 9.9초에 STOP(오차 100ms) vs 5초에 STOP(오차 5000ms)
            final BlindTimerScore accurate = BlindTimerScore.ofNormal(
                    Duration.ofSeconds(10), Duration.ofMillis(9900));
            final BlindTimerScore inaccurate = BlindTimerScore.ofNormal(
                    Duration.ofSeconds(10), Duration.ofSeconds(5));

            // when & then
            assertThat(accurate.getValue()).isLessThan(inaccurate.getValue());
        }

        @Test
        void 오차_0은_완벽한_점수이다() {
            // given - 목표 10초, 정확히 10초에 STOP
            final BlindTimerScore perfect = BlindTimerScore.ofNormal(
                    Duration.ofSeconds(10), Duration.ofSeconds(10));

            // when & then
            assertThat(perfect.getValue()).isEqualTo(0L);
        }

        @Test
        void 목표보다_빨리_STOP해도_오차는_양수이다() {
            // given - 목표 10초, 8초에 STOP(오차 2000ms)
            final BlindTimerScore early = BlindTimerScore.ofNormal(
                    Duration.ofSeconds(10), Duration.ofSeconds(8));

            // when & then
            assertThat(early.getValue()).isEqualTo(2000L);
        }
    }

    @Nested
    class 타임아웃_점수 {

        @Test
        void 타임아웃은_항상_정상_STOP보다_값이_크다() {
            // given - 최대 오차: 목표 19990ms에서 0ms에 STOP
            final BlindTimerScore worstNormal = BlindTimerScore.ofNormal(
                    Duration.ofMillis(19990), Duration.ZERO);
            final BlindTimerScore timeout = BlindTimerScore.ofTimeout();

            // when & then
            assertThat(worstNormal.getValue()).isLessThan(timeout.getValue());
        }
    }

    @Nested
    class 정렬 {

        @Test
        void fromAscending으로_정렬하면_정확한_사람이_앞에_온다() {
            // given - 목표 10초
            final BlindTimerScore accurate = BlindTimerScore.ofNormal(
                    Duration.ofSeconds(10), Duration.ofMillis(9950));
            final BlindTimerScore inaccurate = BlindTimerScore.ofNormal(
                    Duration.ofSeconds(10), Duration.ofSeconds(7));

            // when
            final int comparison = accurate.compareTo(inaccurate);

            // then
            assertThat(comparison).isLessThan(0);
        }

        @Test
        void fromAscending으로_정렬하면_정상_STOP이_타임아웃보다_앞에_온다() {
            // given
            final BlindTimerScore normal = BlindTimerScore.ofNormal(
                    Duration.ofSeconds(10), Duration.ofMillis(25000));
            final BlindTimerScore timeout = BlindTimerScore.ofTimeout();

            // when
            final int comparison = normal.compareTo(timeout);

            // then
            assertThat(comparison).isLessThan(0);
        }
    }
}
