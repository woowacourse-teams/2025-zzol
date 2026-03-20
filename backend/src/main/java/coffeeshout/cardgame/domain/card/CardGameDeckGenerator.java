package coffeeshout.cardgame.domain.card;

public interface CardGameDeckGenerator {

    Deck generate(int additionCardCount, int multiplierCardCount, long seed);
}
