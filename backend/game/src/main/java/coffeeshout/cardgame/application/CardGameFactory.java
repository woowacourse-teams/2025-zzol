package coffeeshout.cardgame.application;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.card.CardGameRandomDeckGenerator;
import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameType;
import org.springframework.stereotype.Component;

@Component
public class CardGameFactory implements MiniGameFactory {

    private static final CardGameRandomDeckGenerator DECK_GENERATOR = new CardGameRandomDeckGenerator();

    @Override
    public MiniGameType type() {
        return MiniGameType.CARD_GAME;
    }

    @Override
    public Playable create(String joinCode) {
        final long seed = Integer.toUnsignedLong(joinCode.hashCode());
        return new CardGame(DECK_GENERATOR, seed);
    }
}
