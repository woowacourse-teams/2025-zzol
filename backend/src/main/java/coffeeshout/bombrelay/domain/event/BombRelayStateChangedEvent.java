package coffeeshout.bombrelay.domain.event;

import coffeeshout.bombrelay.domain.BombRelayGame;

public record BombRelayStateChangedEvent(
        String joinCode,
        String state,
        int currentRound,
        int maxRounds,
        String currentWord,
        String currentTurnPlayerName
) {

    public static BombRelayStateChangedEvent of(BombRelayGame game, String joinCode) {
        final String turnPlayerName = game.getSurvivorCount() > 0
                ? game.getCurrentTurnPlayer().getName()
                : "";
        return new BombRelayStateChangedEvent(
                joinCode,
                game.getState().name(),
                game.getCurrentRound(),
                game.getMaxRounds(),
                game.getCurrentWord(),
                turnPlayerName
        );
    }
}
