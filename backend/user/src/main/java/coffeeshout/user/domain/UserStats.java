package coffeeshout.user.domain;

public class UserStats {

    private final Long userId;
    private int winCount;
    private int survivalStreak;

    public UserStats(Long userId, int winCount, int survivalStreak) {
        this.userId = userId;
        this.winCount = winCount;
        this.survivalStreak = survivalStreak;
    }

    public static UserStats empty(Long userId) {
        return new UserStats(userId, 0, 0);
    }

    public void recordWin() {
        winCount++;
        survivalStreak = 0;
    }

    public void recordSurvival() {
        survivalStreak++;
    }

    public Long getUserId() {
        return userId;
    }

    public int getWinCount() {
        return winCount;
    }

    public int getSurvivalStreak() {
        return survivalStreak;
    }
}
