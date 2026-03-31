package coffeeshout.numberpoker.domain;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.numberpoker.domain.NumberPokerErrorCode;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PokerCardTest {

    @Nested
    class 유효한_카드_생성 {

        @ParameterizedTest
        @ValueSource(ints = {1, 5, 10})
        void 범위_내_값으로_카드를_생성한다(int value) {
            final PokerCard card = new PokerCard(value);

            assertThat(card.value()).isEqualTo(value);
        }
    }

    @Nested
    class 유효하지_않은_카드_생성 {

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 11, 100})
        void 범위_밖_값은_예외가_발생한다(int value) {
            assertCoffeeShoutException(
                    () -> new PokerCard(value),
                    NumberPokerErrorCode.INVALID_CARD_VALUE
            );
        }
    }

    @Nested
    class 카드_동등성 {

        @Test
        void 같은_값의_카드는_동일하다() {
            final PokerCard card1 = new PokerCard(5);
            final PokerCard card2 = new PokerCard(5);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(card1).isEqualTo(card2);
                softly.assertThat(card1.hashCode()).isEqualTo(card2.hashCode());
            });
        }

        @Test
        void 다른_값의_카드는_동일하지_않다() {
            final PokerCard card1 = new PokerCard(5);
            final PokerCard card2 = new PokerCard(6);

            assertThat(card1).isNotEqualTo(card2);
        }
    }
}
