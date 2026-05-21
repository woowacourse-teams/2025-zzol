package coffeeshout.cardgame.application;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.card.CardGameDeckGenerator;
import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardGameFactory implements MiniGameFactory {

    private final CardGameDeckGenerator deckGenerator;

    @Override
    public MiniGameType type() {
        return MiniGameType.CARD_GAME;
    }

    @Override
    public Playable create(String joinCode) {
        final long seed = Integer.toUnsignedLong(joinCode.hashCode());
        return new CardGame(deckGenerator, seed);
    }
}
