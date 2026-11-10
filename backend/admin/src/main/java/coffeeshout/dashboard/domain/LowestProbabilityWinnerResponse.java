package coffeeshout.dashboard.domain;

import java.util.List;

public record LowestProbabilityWinnerResponse(Double probability, List<PlayerInfo> players) {
    public record PlayerInfo(String nickname, String userCode, boolean masked) {

        public PlayerInfo(String nickname, String userCode) {
            this(nickname, userCode, false);
        }

        public PlayerInfo mask() {
            return new PlayerInfo(MaskedNickname.TEXT, userCode, true);
        }
    }

    public static LowestProbabilityWinnerResponse of(Integer dbProbability, List<PlayerInfo> players) {
        return new LowestProbabilityWinnerResponse(dbProbability / 100.0, players);
    }

    public static LowestProbabilityWinnerResponse empty() {
        return new LowestProbabilityWinnerResponse(0.0, List.of());
    }
}
