package coffeeshout.minigame.cardgame.domain.cardgame.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.cardgame.domain.card.AdditionCard;
import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.cardgame.domain.card.Deck;
import coffeeshout.cardgame.domain.card.MultiplierCard;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DeckTest {

    private List<Card> additionCards;
    private List<Card> multiplierCards;
    private Deck deck;

    @BeforeEach
    void setUp() {
        additionCards = List.of(
                AdditionCard.PLUS_40,
                AdditionCard.PLUS_30,
                AdditionCard.PLUS_20,
                AdditionCard.PLUS_10,
                AdditionCard.ZERO,
                AdditionCard.MINUS_10,
                AdditionCard.MINUS_20
        );
        multiplierCards = List.of(
                MultiplierCard.QUADRUPLE,
                MultiplierCard.DOUBLE
        );
        deck = new Deck(additionCards, multiplierCards);
    }

    @Nested
    class 덱_생성_테스트 {

        @Test
        void 덱이_생성된다() {
            // given & when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(deck.getCards()).hasSize(9);
                softly.assertThat(deck.getPickedCards()).isEmpty();
                softly.assertThat(deck.getCards()).containsAll(additionCards);
                softly.assertThat(deck.getCards()).containsAll(multiplierCards);
            });
        }

        @Test
        void 카드를_섞는다() {
            // given
            List<Card> originalOrder = deck.getCards();

            // when
            deck.shuffle();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(deck.getCards()).hasSize(originalOrder.size());
                softly.assertThat(deck.getCards()).containsExactlyInAnyOrderElementsOf(originalOrder);
            });
        }
    }

    @Nested
    class 인덱스_카드_선택_테스트 {

        @Test
        void 인덱스로_카드를_선택한다() {
            // given
            int cardIndex = 0;

            // when
            Card pickedCard = deck.pick(cardIndex);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(pickedCard).isEqualTo(deck.getCards().get(cardIndex));
                softly.assertThat(deck.getPickedCards()).hasSize(1);
                softly.assertThat(deck.getPickedCards()).contains(pickedCard);
            });
        }

        @Test
        void 이미_선택한_카드를_다시_선택하면_예외가_발생한다() {
            // given
            int cardIndex = 0;
            deck.pick(cardIndex);

            // when & then
            assertThatThrownBy(() -> deck.pick(cardIndex))
                    .isInstanceOf(IllegalStateException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 9, 100})
        void 유효하지_않은_인덱스로_카드를_선택하면_예외가_발생한다(int invalidIndex) {
            // when & then
            assertThatThrownBy(() -> deck.pick(invalidIndex))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }

        @Test
        void 여러_카드를_순차적으로_선택한다() {
            // when
            Card firstCard = deck.pick(0);
            Card secondCard = deck.pick(1);
            Card thirdCard = deck.pick(2);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(deck.getPickedCards()).hasSize(3);
                softly.assertThat(deck.getPickedCards()).containsExactly(firstCard, secondCard, thirdCard);
            });
        }
    }

    @Nested
    class 랜덤_카드_선택_테스트 {

        @Test
        void 랜덤으로_카드를_선택한다() {
            // when
            Card pickedCard = deck.pickRandom();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(pickedCard).isNotNull();
                softly.assertThat(deck.getPickedCards()).hasSize(1);
                softly.assertThat(deck.getPickedCards()).contains(pickedCard);
                softly.assertThat(deck.getCards()).contains(pickedCard);
            });
        }

        @Test
        void 랜덤_선택과_인덱스_선택을_함께_사용한다() {
            // given
            Card indexPickedCard = deck.pick(0);
            Card randomPickedCard = deck.pickRandom();

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(deck.getPickedCards()).hasSize(2);
                softly.assertThat(deck.getPickedCards()).contains(indexPickedCard);
                softly.assertThat(deck.getPickedCards()).contains(randomPickedCard);
                softly.assertThat(indexPickedCard).isNotEqualTo(randomPickedCard);
            });
        }
    }

    @Nested
    class 카드_선택_상태_확인_테스트 {

        @Test
        void 카드가_선택되었는지_확인한다() {
            // given
            Card card = deck.getCards().get(0);

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(deck.getPickedCards().contains(card)).isFalse();
                deck.pick(0);
                softly.assertThat(deck.getPickedCards().contains(card)).isTrue();
            });
        }

        @Test
        void 덱에_없는_카드의_선택_여부를_확인한다() {
            // given
            Card cardNotInDeck = new AdditionCard(999);

            // when & then
            assertThat(deck.getPickedCards().contains(cardNotInDeck)).isFalse();
        }
    }
}
