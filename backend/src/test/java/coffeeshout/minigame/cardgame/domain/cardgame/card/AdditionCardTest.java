package coffeeshout.minigame.cardgame.domain.cardgame.card;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.cardgame.domain.card.AdditionCard;
import coffeeshout.cardgame.domain.card.CardType;
import coffeeshout.cardgame.domain.card.MultiplierCard;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class AdditionCardTest {

    @Test
    void 덧셈_카드를_생성한다() {
        // given
        int value = 50;

        // when
        AdditionCard card = new AdditionCard(value);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(card.getType()).isEqualTo(CardType.ADDITION);
            softly.assertThat(card.getValue()).isEqualTo(value);
        });
    }

    @Test
    void 같은_값의_덧셈_카드는_동일하다() {
        // given
        AdditionCard card1 = new AdditionCard(25);
        AdditionCard card2 = new AdditionCard(25);

        // when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(card1).isEqualTo(card2);
            softly.assertThat(card1.hashCode()).isEqualTo(card2.hashCode());
        });
    }

    @Test
    void 다른_값의_덧셈_카드는_다르다() {
        // given
        AdditionCard card1 = new AdditionCard(25);
        AdditionCard card2 = new AdditionCard(30);

        // when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(card1).isNotEqualTo(card2);
            softly.assertThat(card1.hashCode()).isNotEqualTo(card2.hashCode());
        });
    }

    @Test
    void 덧셈_카드와_곱셈_카드는_값이_같아도_다르다() {
        // given
        AdditionCard additionCard = new AdditionCard(2);
        MultiplierCard multiplierCard = new MultiplierCard(2);

        // when & then
        assertThat(additionCard).isNotEqualTo(multiplierCard);
    }

    @Test
    void 음수_값으로_덧셈_카드를_생성한다() {
        // given
        int negativeValue = -15;

        // when
        AdditionCard card = new AdditionCard(negativeValue);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(card.getType()).isEqualTo(CardType.ADDITION);
            softly.assertThat(card.getValue()).isEqualTo(negativeValue);
        });
    }

    @Test
    void 영값으로_덧셈_카드를_생성한다() {
        // given
        int zeroValue = 0;

        // when
        AdditionCard card = new AdditionCard(zeroValue);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(card.getType()).isEqualTo(CardType.ADDITION);
            softly.assertThat(card.getValue()).isEqualTo(zeroValue);
        });
    }
}
