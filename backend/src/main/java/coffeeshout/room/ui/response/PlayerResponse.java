package coffeeshout.room.ui.response;

import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.domain.roulette.Probability;

public record PlayerResponse(
        String playerName,
        PlayerType playerType,
        Boolean isReady,
        Integer colorIndex,
        Double probability
) {

    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getName().value(),
                player.getPlayerType(),
                player.getIsReady(),
                player.getColorIndex(),
                parseProbability(player.getProbability())
        );
    }

    private static Double parseProbability(Probability probability) {
        return probability.value() / 100.0;
    }
}
