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
            // given
            final BlindTimerScore accurate = BlindTimerScore.ofNormal(Duration.ofMillis(100));
            final BlindTimerScore inaccurate = BlindTimerScore.ofNormal(Duration.ofMillis(5000));

            // when & then
            assertThat(accurate.getValue()).isLessThan(inaccurate.getValue());
        }

        @Test
        void 오차_0은_완벽한_점수이다() {
            // given
            final BlindTimerScore perfect = BlindTimerScore.ofNormal(Duration.ZERO);

            // when & then
            assertThat(perfect.getValue()).isEqualTo(0L);
        }
    }

    @Nested
    class 타임아웃_점수 {

        @Test
        void 타임아웃은_항상_정상_STOP보다_값이_크다() {
            // given - 최대 오차: 목표 19990ms에서 0ms에 STOP (불가능하지만 이론적 최대)
            final BlindTimerScore worstNormal = BlindTimerScore.ofNormal(Duration.ofMillis(19990));
            final BlindTimerScore timeout = BlindTimerScore.ofTimeout();

            // when & then
            assertThat(worstNormal.getValue()).isLessThan(timeout.getValue());
        }
    }

    @Nested
    class 정렬 {

        @Test
        void fromAscending으로_정렬하면_정확한_사람이_앞에_온다() {
            // given
            final BlindTimerScore accurate = BlindTimerScore.ofNormal(Duration.ofMillis(50));
            final BlindTimerScore inaccurate = BlindTimerScore.ofNormal(Duration.ofMillis(3000));

            // when
            final int comparison = accurate.compareTo(inaccurate);

            // then
            assertThat(comparison).isLessThan(0);
        }

        @Test
        void fromAscending으로_정렬하면_정상_STOP이_타임아웃보다_앞에_온다() {
            // given
            final BlindTimerScore normal = BlindTimerScore.ofNormal(Duration.ofMillis(15000));
            final BlindTimerScore timeout = BlindTimerScore.ofTimeout();

            // when
            final int comparison = normal.compareTo(timeout);

            // then
            assertThat(comparison).isLessThan(0);
        }
    }
}
