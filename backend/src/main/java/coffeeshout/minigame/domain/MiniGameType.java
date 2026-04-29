package coffeeshout.minigame.domain;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.card.CardGameRandomDeckGenerator;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.room.domain.Playable;
import coffeeshout.speedtouch.domain.SpeedTouchGame;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MiniGameType {

    CARD_GAME("카드게임"),
    RACING_GAME("레이싱"),
    SPEED_TOUCH("스피드터치"),
    BLIND_TIMER("블라인드타이머"),
    BLOCK_STACKING("블록쌓기"),
    ;

    public final String label;

    public Playable createMiniGame(String joinCode) {
        Objects.requireNonNull(joinCode, "joinCode는 null이 아니어야 합니다.");
        return switch (this) {
            case CARD_GAME -> {
                final long seed = Integer.toUnsignedLong(joinCode.hashCode());
                yield new CardGame(new CardGameRandomDeckGenerator(), seed);
            }
            case RACING_GAME -> new RacingGame();
            case SPEED_TOUCH -> new SpeedTouchGame();
            case BLIND_TIMER -> new BlindTimerGame();
            case BLOCK_STACKING -> new BlockStackingGame();
        };
    }
}
