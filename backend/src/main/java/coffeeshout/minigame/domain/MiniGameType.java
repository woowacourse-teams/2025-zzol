package coffeeshout.minigame.domain;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.card.CardGameRandomDeckGenerator;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.room.domain.Playable;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MiniGameType {

    CARD_GAME,
    RACING_GAME,
    ;

    public Playable createMiniGame(String joinCode) {
        Objects.requireNonNull(joinCode, "joinCode는 null이 아니어야 합니다.");
        return switch (this) {
            case CARD_GAME -> {
                final long seed = Integer.toUnsignedLong(joinCode.hashCode());
                yield new CardGame(new CardGameRandomDeckGenerator(), seed);
            }
            case RACING_GAME -> new RacingGame();
        };
    }
}
