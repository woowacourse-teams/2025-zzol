package coffeeshout.bombrelay.domain.event;

import coffeeshout.bombrelay.domain.BombRelayGame;

public record BombRelayFinishedEvent(
        String joinCode,
        String state
) {

    public static BombRelayFinishedEvent of(BombRelayGame game, String joinCode) {
        return new BombRelayFinishedEvent(joinCode, game.getState().name());
    }
}
