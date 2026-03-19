package coffeeshout.bombrelay.domain.event;

import coffeeshout.bombrelay.domain.BombRelayGame;
import coffeeshout.bombrelay.domain.BombRelayGameState;

public record BombRelayFinishedEvent(
        String joinCode,
        BombRelayGameState state
) {

    public static BombRelayFinishedEvent of(BombRelayGame game, String joinCode) {
        return new BombRelayFinishedEvent(joinCode, game.getState());
    }
}
