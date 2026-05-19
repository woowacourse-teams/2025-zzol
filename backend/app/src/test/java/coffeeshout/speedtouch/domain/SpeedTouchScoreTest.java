package coffeeshout.speedtouch.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SpeedTouchScoreTest {

    @Nested
    class 완주자_점수 {

        @Test
        void 완주_시간이_짧을수록_값이_작다() {
            // given
            final SpeedTouchScore fast = SpeedTouchScore.ofFinished(5000L);
            final SpeedTouchScore slow = SpeedTouchScore.ofFinished(15000L);

            // when & then
            assertThat(fast.getValue()).isLessThan(slow.getValue());
        }
    }

    @Nested
    class DNF_점수 {

        @Test
        void 진행도가_높을수록_값이_작다() {
            // given
            final SpeedTouchScore moreProgress = SpeedTouchScore.ofDnf(20);
            final SpeedTouchScore lessProgress = SpeedTouchScore.ofDnf(10);

            // when & then
            assertThat(moreProgress.getValue()).isLessThan(lessProgress.getValue());
        }
    }

    @Nested
    class 완주자_vs_DNF {

        @Test
        void 완주자는_항상_DNF보다_값이_작다() {
            // given - 가장 느린 완주자 (30초 = 30000ms)
            final SpeedTouchScore slowestFinisher = SpeedTouchScore.ofFinished(30000L);
            // given - 가장 많이 진행한 DNF (24/25)
            final SpeedTouchScore bestDnf = SpeedTouchScore.ofDnf(24);

            // when & then
            assertThat(slowestFinisher.getValue()).isLessThan(bestDnf.getValue());
        }

        @Test
        void fromAscending으로_정렬하면_완주자가_앞에_온다() {
            // given
            final SpeedTouchScore finisher = SpeedTouchScore.ofFinished(25000L);
            final SpeedTouchScore dnf = SpeedTouchScore.ofDnf(24);

            // when - compareTo 기준 오름차순
            final int comparison = finisher.compareTo(dnf);

            // then
            assertThat(comparison).isLessThan(0);
        }
    }
}
