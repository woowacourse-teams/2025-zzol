package coffeeshout.fixture;

import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.cardgame.domain.card.Deck;
import java.util.List;

public class StubDeck extends Deck {

    public StubDeck(
            List<Card> additionCards,
            List<Card> multiplierCards
    ) {
        super(additionCards, multiplierCards);
    }

    @Override
    public void shuffle() {
    }
}
