package coffeeshout.user.ui.response;

import coffeeshout.user.domain.UserStats;

public record UserStatsResponse(
        int winCount,
        int survivalStreak
) {
    public static UserStatsResponse from(UserStats stats) {
        return new UserStatsResponse(stats.getWinCount(), stats.getSurvivalStreak());
    }
}
