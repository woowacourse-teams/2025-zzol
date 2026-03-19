package coffeeshout.bombrelay.domain.event;

import coffeeshout.bombrelay.domain.BombRelayGame;
import coffeeshout.bombrelay.domain.BombRelayPlayer;
import java.util.List;

public record BombRelayProgressEvent(
        String joinCode,
        String currentWord,
        String currentTurnPlayerName,
        int currentRound,
        List<PlayerProgress> players
) {

    public record PlayerProgress(
            String playerName,
            boolean eliminated,
            int eliminatedRound
    ) {
    }

    public static BombRelayProgressEvent of(BombRelayGame game, String joinCode) {
        final List<PlayerProgress> playerProgresses = game.getPlayers().getAll().stream()
                .map(p -> new PlayerProgress(p.getName(), p.isEliminated(), p.getEliminatedRound()))
                .toList();

        final String turnPlayerName = game.getSurvivorCount() > 0
                ? game.getCurrentTurnPlayer().getName()
                : "";

        return new BombRelayProgressEvent(
                joinCode,
                game.getCurrentWord(),
                turnPlayerName,
                game.getCurrentRound(),
                playerProgresses
        );
    }
}
