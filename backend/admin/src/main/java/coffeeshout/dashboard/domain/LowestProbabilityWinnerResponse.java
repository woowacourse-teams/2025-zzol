package coffeeshout.dashboard.domain;

import java.util.List;

public record LowestProbabilityWinnerResponse(Double probability, List<RankedMember> players) {

    public static LowestProbabilityWinnerResponse of(long dbProbability, List<RankedMember> players) {
        return new LowestProbabilityWinnerResponse(dbProbability / 100.0, players);
    }

    public static LowestProbabilityWinnerResponse empty() {
        return new LowestProbabilityWinnerResponse(0.0, List.of());
    }
}
