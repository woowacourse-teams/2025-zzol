package coffeeshout.minigame.domain;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.bombrelay.domain.BombRelayGame;
import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.card.CardGameRandomDeckGenerator;
import coffeeshout.laddergame.domain.LadderGame;
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
    BOMB_RELAY("폭탄릴레이"),
    BLOCK_STACKING("블록쌓기"),
    LADDER_GAME("사다리타기"),
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
            case BOMB_RELAY -> new BombRelayGame();
            case BLOCK_STACKING -> new BlockStackingGame();
            case LADDER_GAME -> new LadderGame();
        };
    }
}
