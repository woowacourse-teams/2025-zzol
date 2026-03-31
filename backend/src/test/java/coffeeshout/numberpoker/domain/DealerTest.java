package coffeeshout.numberpoker.domain;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DealerTest {

    Dealer dealer;
    PokerCard firstCard = new PokerCard(7);
    PokerCard secondCard = new PokerCard(3);

    @BeforeEach
    void setUp() {
        dealer = new Dealer(firstCard, secondCard);
    }

    @Nested
    class 초기_상태 {

        @Test
        void 카드가_모두_가려져_있다() {
            assertThat(dealer.getVisibleCards()).isEmpty();
        }

        @Test
        void 아직_전체_공개_상태가_아니다() {
            assertThat(dealer.isFullyRevealed()).isFalse();
        }
    }

    @Nested
    class 첫_번째_카드_공개 {

        @Test
        void 첫_번째_카드_한_장만_공개된다() {
            dealer.revealFirst();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(dealer.getVisibleCards()).hasSize(1);
                softly.assertThat(dealer.getVisibleCards()).containsExactly(firstCard);
                softly.assertThat(dealer.isFullyRevealed()).isFalse();
            });
        }

        @Test
        void 이미_공개된_상태에서_다시_공개하면_예외가_발생한다() {
            dealer.revealFirst();

            assertCoffeeShoutException(
                    () -> dealer.revealFirst(),
                    NumberPokerErrorCode.INVALID_PHASE_ACTION
            );
        }
    }

    @Nested
    class 전체_공개 {

        @Test
        void 두_장_모두_공개된다() {
            dealer.revealAll();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(dealer.getVisibleCards()).hasSize(2);
                softly.assertThat(dealer.getVisibleCards()).containsExactly(firstCard, secondCard);
                softly.assertThat(dealer.isFullyRevealed()).isTrue();
            });
        }

        @Test
        void 첫_번째_공개_없이도_전체_공개할_수_있다() {
            dealer.revealAll();

            assertThat(dealer.getVisibleCards()).hasSize(2);
        }
    }

    @Nested
    class 패_강도_평가 {

        @Test
        void 딜러_패의_핸드_랭킹을_반환한다() {
            final Dealer pairDealer = new Dealer(new PokerCard(5), new PokerCard(5));

            assertThat(pairDealer.getHandRanking().isPair()).isTrue();
        }
    }
}
