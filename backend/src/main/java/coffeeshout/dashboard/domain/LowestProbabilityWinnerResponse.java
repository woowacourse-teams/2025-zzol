package coffeeshout.dashboard.domain;

import java.util.List;

public record LowestProbabilityWinnerResponse(
        Double probability,
        List<PlayerInfo> players
) {
    public record PlayerInfo(String nickname, String userCode) {
    }

    public static LowestProbabilityWinnerResponse of(Integer dbProbability, List<PlayerInfo> players) {
        return new LowestProbabilityWinnerResponse(
                dbProbability / 100.0,
                players
        );
    }

    public static LowestProbabilityWinnerResponse empty() {
        return new LowestProbabilityWinnerResponse(0.0, List.of());
    }
}
