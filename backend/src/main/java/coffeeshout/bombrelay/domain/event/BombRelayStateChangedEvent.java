package coffeeshout.bombrelay.domain.event;

import coffeeshout.bombrelay.domain.BombRelayGame;

public record BombRelayStateChangedEvent(
        String joinCode,
        String state,
        int currentRound,
        int maxRounds,
        String currentWord,
        String currentTurnPlayerName,
        String eliminatedPlayerName
) {

    public static BombRelayStateChangedEvent of(BombRelayGame game, String joinCode) {
        return of(game, joinCode, null);
    }

    public static BombRelayStateChangedEvent of(BombRelayGame game, String joinCode, String eliminatedPlayerName) {
        final String turnPlayerName = game.getSurvivorCount() > 0
                ? game.getCurrentTurnPlayer().getName()
                : "";
        return new BombRelayStateChangedEvent(
                joinCode,
                game.getState().name(),
                game.getCurrentRound(),
                game.getMaxRounds(),
                game.getCurrentWord(),
                turnPlayerName,
                eliminatedPlayerName
        );
    }
}
