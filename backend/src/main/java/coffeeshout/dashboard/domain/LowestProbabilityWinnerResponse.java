package coffeeshout.dashboard.domain;

import java.util.List;

public record LowestProbabilityWinnerResponse(
        Double probability,
        List<String> playerNames
) {
    public static LowestProbabilityWinnerResponse of(Integer dbProbability, List<String> playerNames) {
        return new LowestProbabilityWinnerResponse(
                dbProbability / 100.0,
                playerNames
        );
    }
}
