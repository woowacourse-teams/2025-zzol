package coffeeshout.minigame.cardgame.domain.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.cardgame.domain.card.CardGameDeckGenerator;
import coffeeshout.cardgame.domain.card.CardGameRandomDeckGenerator;
import coffeeshout.cardgame.domain.card.CardType;
import coffeeshout.cardgame.domain.card.Deck;
import org.junit.jupiter.api.Test;

class CardGameRandomDeckGeneratorTest {

    @Test
    void 카드를_랜덤으로_9장_뽑는다() {
        // given
        int additionCardCount = 6;
        int multiplierCardCount = 3;

        final CardGameDeckGenerator cardGameDeck = new CardGameRandomDeckGenerator();
        Deck deck = cardGameDeck.generate(additionCardCount, multiplierCardCount, 1234L);

        // when & then
        assertThat(deck.size()).isEqualTo(9);

        long generalCardCount = deck.stream()
                .filter(card -> card.getType() == CardType.ADDITION)
                .count();
        assertThat(generalCardCount).isEqualTo(6);

        long specialCardCount = deck.stream()
                .filter(card -> card.getType() == CardType.MULTIPLIER)
                .count();
        assertThat(specialCardCount).isEqualTo(3);
    }

}
