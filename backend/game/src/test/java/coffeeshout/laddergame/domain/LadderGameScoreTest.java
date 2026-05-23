package coffeeshout.laddergame.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LadderGameScoreTest {

    @Nested
    class getValue_테스트 {

        @Test
        void rank_값을_long으로_반환한다() {
            final LadderGameScore score = new LadderGameScore(1);

            assertThat(score.getValue()).isEqualTo(1L);
        }

        @Test
        void rank_3을_3으로_반환한다() {
            final LadderGameScore score = new LadderGameScore(3);

            assertThat(score.getValue()).isEqualTo(3L);
        }
    }

    @Nested
    class equals_테스트 {

        @Test
        void 같은_rank면_equals가_true다() {
            final LadderGameScore score1 = new LadderGameScore(2);
            final LadderGameScore score2 = new LadderGameScore(2);

            assertThat(score1).isEqualTo(score2);
        }

        @Test
        void 다른_rank면_equals가_false다() {
            final LadderGameScore score1 = new LadderGameScore(1);
            final LadderGameScore score2 = new LadderGameScore(3);

            assertThat(score1).isNotEqualTo(score2);
        }
    }

    @Nested
    class compareTo_테스트 {

        @Test
        void rank가_낮은_순위가_더_작다() {
            final LadderGameScore rank1 = new LadderGameScore(1);
            final LadderGameScore rank3 = new LadderGameScore(3);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(rank1.compareTo(rank3)).isNegative();
                softly.assertThat(rank3.compareTo(rank1)).isPositive();
            });
        }

        @Test
        void 같은_rank면_compareTo가_0이다() {
            final LadderGameScore score1 = new LadderGameScore(2);
            final LadderGameScore score2 = new LadderGameScore(2);

            assertThat(score1).isEqualByComparingTo(score2);
        }
    }
}
