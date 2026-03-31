package coffeeshout.numberpoker.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HandRankingTest {

    @Nested
    class 패_분류 {

        @Test
        void 같은_숫자_두_장은_페어다() {
            final HandRanking ranking = HandRanking.of(new PokerCard(5), new PokerCard(5));

            assertThat(ranking.isPair()).isTrue();
        }

        @Test
        void 다른_숫자_두_장은_하이카드다() {
            final HandRanking ranking = HandRanking.of(new PokerCard(8), new PokerCard(3));

            assertThat(ranking.isPair()).isFalse();
        }
    }

    @Nested
    class 패_비교 {

        @Test
        void 페어는_하이카드를_이긴다() {
            final HandRanking pair = HandRanking.of(new PokerCard(3), new PokerCard(3));
            final HandRanking highCard = HandRanking.of(new PokerCard(9), new PokerCard(8));

            assertThat(pair.compareTo(highCard)).isGreaterThan(0);
        }

        @Test
        void 높은_숫자의_페어가_낮은_숫자의_페어를_이긴다() {
            final HandRanking highPair = HandRanking.of(new PokerCard(8), new PokerCard(8));
            final HandRanking lowPair = HandRanking.of(new PokerCard(5), new PokerCard(5));

            assertThat(highPair.compareTo(lowPair)).isGreaterThan(0);
        }

        @Test
        void 하이카드는_높은_숫자로_비교한다() {
            final HandRanking higher = HandRanking.of(new PokerCard(9), new PokerCard(3));
            final HandRanking lower = HandRanking.of(new PokerCard(8), new PokerCard(7));

            assertThat(higher.compareTo(lower)).isGreaterThan(0);
        }

        @Test
        void 하이카드_높은_숫자가_같으면_낮은_숫자로_비교한다() {
            final HandRanking higher = HandRanking.of(new PokerCard(9), new PokerCard(6));
            final HandRanking lower = HandRanking.of(new PokerCard(9), new PokerCard(3));

            assertThat(higher.compareTo(lower)).isGreaterThan(0);
        }

        @Test
        void 완전히_같은_패는_동점이다() {
            SoftAssertions.assertSoftly(softly -> {
                final HandRanking pair1 = HandRanking.of(new PokerCard(5), new PokerCard(5));
                final HandRanking pair2 = HandRanking.of(new PokerCard(5), new PokerCard(5));
                softly.assertThat(pair1.compareTo(pair2)).isEqualTo(0);

                final HandRanking high1 = HandRanking.of(new PokerCard(8), new PokerCard(3));
                final HandRanking high2 = HandRanking.of(new PokerCard(8), new PokerCard(3));
                softly.assertThat(high1.compareTo(high2)).isEqualTo(0);
            });
        }

        @Test
        void 카드_입력_순서에_관계없이_같은_패로_평가한다() {
            final HandRanking h1 = HandRanking.of(new PokerCard(8), new PokerCard(3));
            final HandRanking h2 = HandRanking.of(new PokerCard(3), new PokerCard(8));

            assertThat(h1.compareTo(h2)).isEqualTo(0);
        }
    }
}
