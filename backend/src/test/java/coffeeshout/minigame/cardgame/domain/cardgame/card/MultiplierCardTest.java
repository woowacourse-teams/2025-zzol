package coffeeshout.minigame.cardgame.domain.cardgame.card;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.cardgame.domain.card.AdditionCard;
import coffeeshout.cardgame.domain.card.CardType;
import coffeeshout.cardgame.domain.card.MultiplierCard;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class MultiplierCardTest {

    @Test
    void 곱셈_카드를_생성한다() {
        // given
        int value = 3;

        // when
        MultiplierCard card = new MultiplierCard(value);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(card.getType()).isEqualTo(CardType.MULTIPLIER);
            softly.assertThat(card.getValue()).isEqualTo(value);
        });
    }

    @Test
    void 같은_값의_곱셈_카드는_동일하다() {
        // given
        MultiplierCard card1 = new MultiplierCard(3);
        MultiplierCard card2 = new MultiplierCard(3);

        // when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(card1).isEqualTo(card2);
            softly.assertThat(card1.hashCode()).isEqualTo(card2.hashCode());
        });
    }

    @Test
    void 다른_값의_곱셈_카드는_다르다() {
        // given
        MultiplierCard card1 = new MultiplierCard(2);
        MultiplierCard card2 = new MultiplierCard(4);

        // when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(card1).isNotEqualTo(card2);
            softly.assertThat(card1.hashCode()).isNotEqualTo(card2.hashCode());
        });
    }

    @Test
    void 곱셈_카드와_덧셈_카드는_값이_같아도_다르다() {
        // given
        MultiplierCard multiplierCard = new MultiplierCard(2);
        AdditionCard additionCard = new AdditionCard(2);

        // when & then
        assertThat(multiplierCard).isNotEqualTo(additionCard);
    }

    @Test
    void 정적_카드들이_모두_곱셈_타입이다() {
        // when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(MultiplierCard.QUADRUPLE.getType()).isEqualTo(CardType.MULTIPLIER);
            softly.assertThat(MultiplierCard.DOUBLE.getType()).isEqualTo(CardType.MULTIPLIER);
            softly.assertThat(MultiplierCard.INVERT.getType()).isEqualTo(CardType.MULTIPLIER);
        });
    }

    @Test
    void 음수_값으로_곱셈_카드를_생성한다() {
        // given
        int negativeValue = -2;

        // when
        MultiplierCard card = new MultiplierCard(negativeValue);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(card.getType()).isEqualTo(CardType.MULTIPLIER);
            softly.assertThat(card.getValue()).isEqualTo(negativeValue);
        });
    }

    @Test
    void 영값으로_곱셈_카드를_생성한다() {
        // given
        int zeroValue = 0;

        // when
        MultiplierCard card = new MultiplierCard(zeroValue);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(card.getType()).isEqualTo(CardType.MULTIPLIER);
            softly.assertThat(card.getValue()).isEqualTo(zeroValue);
        });
    }
}
