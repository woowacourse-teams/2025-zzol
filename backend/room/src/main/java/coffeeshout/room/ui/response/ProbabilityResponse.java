package coffeeshout.room.ui.response;

import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.roulette.Probability;

public record ProbabilityResponse(
        String playerName,
        Double probability
) {

    public static ProbabilityResponse from(Player player) {
        return new ProbabilityResponse(player.getName().value(), parseProbability(player.getProbability()));
    }

    private static Double parseProbability(Probability probability) {
        return probability.value() / 100.0;
    }
}
