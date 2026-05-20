package coffeeshout.blockstacking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BlockStackingScoreTest {

    @Nested
    class getValue_테스트 {

        @Test
        void floor_값을_long으로_반환한다() {
            final BlockStackingScore score = new BlockStackingScore(7);

            assertThat(score.getValue()).isEqualTo(7L);
        }

        @Test
        void floor가_0이면_0을_반환한다() {
            final BlockStackingScore score = new BlockStackingScore(0);

            assertThat(score.getValue()).isZero();
        }
    }

    @Nested
    class equals_테스트 {

        @Test
        void 같은_floor면_equals가_true다() {
            final BlockStackingScore score1 = new BlockStackingScore(5);
            final BlockStackingScore score2 = new BlockStackingScore(5);

            assertThat(score1).isEqualTo(score2);
        }

        @Test
        void 다른_floor면_equals가_false다() {
            final BlockStackingScore score1 = new BlockStackingScore(3);
            final BlockStackingScore score2 = new BlockStackingScore(5);

            assertThat(score1).isNotEqualTo(score2);
        }
    }

    @Nested
    class compareTo_테스트 {

        @Test
        void 높은_floor가_더_크다() {
            final BlockStackingScore low = new BlockStackingScore(3);
            final BlockStackingScore high = new BlockStackingScore(7);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(high.compareTo(low)).isPositive();
                softly.assertThat(low.compareTo(high)).isNegative();
            });
        }

        @Test
        void 같은_floor면_compareTo가_0이다() {
            final BlockStackingScore score1 = new BlockStackingScore(5);
            final BlockStackingScore score2 = new BlockStackingScore(5);

            assertThat(score1).isEqualByComparingTo(score2);
        }
    }
}
