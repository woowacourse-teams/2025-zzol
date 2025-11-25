package coffeeshout.room.ui.response;

import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.domain.roulette.Probability;

public record PlayerResponse(
        String playerName,
        PlayerMenuResponse menuResponse,
        PlayerType playerType,
        Boolean isReady,
        Integer colorIndex,
        Double probability
) {

    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getName().value(),
                PlayerMenuResponse.from(player),
                player.getPlayerType(),
                player.getIsReady(),
                player.getColorIndex(),
                parseProbability(player.getProbability())
        );
    }

    private static Double parseProbability(Probability probability) {
        return probability.value() / 100.0;
    }

    public record PlayerMenuResponse(
            String name,
            String temperature,
            String categoryImageUrl
    ) {

        public static PlayerMenuResponse from(Player player) {
            return new PlayerMenuResponse(
                    player.getSelectedMenu().menu().getName(),
                    player.getSelectedMenu().menuTemperature().name(),
                    player.getSelectedMenu().menu().getCategoryImageUrl()
            );
        }
    }
}
