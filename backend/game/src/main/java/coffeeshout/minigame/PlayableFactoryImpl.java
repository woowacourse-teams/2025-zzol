package coffeeshout.minigame;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.card.CardGameRandomDeckGenerator;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.Playable;
import coffeeshout.minigame.domain.PlayableFactory;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.speedtouch.domain.SpeedTouchGame;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PlayableFactoryImpl implements PlayableFactory {

    @Override
    public Playable create(MiniGameType type, String joinCode) {
        Objects.requireNonNull(joinCode, "joinCode는 null이 아니어야 합니다.");
        return switch (type) {
            case CARD_GAME -> {
                final long seed = Integer.toUnsignedLong(joinCode.hashCode());
                yield new CardGame(new CardGameRandomDeckGenerator(), seed);
            }
            case RACING_GAME -> new RacingGame();
            case SPEED_TOUCH -> new SpeedTouchGame();
            case BLIND_TIMER -> new BlindTimerGame();
            case BLOCK_STACKING -> new BlockStackingGame();
            case LADDER_GAME -> new LadderGame();
        };
    }
}
