package coffeeshout.cardgame.domain.card;

import java.util.List;
import java.util.Random;

public class CardGameRandomDeckGenerator implements CardGameDeckGenerator {

    private static final AdditionCards ADDITION_CARDS = new AdditionCards();
    private static final MultiplierCards MULTIPLIER_CARDS = new MultiplierCards();

    @Override
    public Deck generate(int additionCardCount, int multiplierCardCount, long seed) {
        final Random random = new Random(seed);
        final List<Card> additionCards = ADDITION_CARDS.pickCards(additionCardCount, random);
        final List<Card> multiplierCards = MULTIPLIER_CARDS.pickCards(multiplierCardCount, random);
        final Deck deck = new Deck(additionCards, multiplierCards);
        deck.shuffle(random);
        return deck;
    }
}
