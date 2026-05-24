package coffeeshout.fixture;

import coffeeshout.cardgame.domain.card.AdditionCard;
import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.cardgame.domain.card.CardGameDeckGenerator;
import coffeeshout.cardgame.domain.card.Deck;
import coffeeshout.cardgame.domain.card.MultiplierCard;
import java.util.List;

public final class CardGameDeckStub implements CardGameDeckGenerator {

    @Override
    public Deck generate(int additionCardCount, int multiplierCardCount, long seed) {
        List<Card> additionCards = List.of(
                new AdditionCard(40),
                new AdditionCard(30),
                new AdditionCard(20),
                new AdditionCard(10),
                new AdditionCard(0),
                new AdditionCard(-10)
        );
        List<Card> multiplierCards = List.of(
                new MultiplierCard(4),
                new MultiplierCard(2),
                new MultiplierCard(0)
        );
        return new StubDeck(additionCards, multiplierCards);
    }
}
