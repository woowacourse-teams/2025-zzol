package coffeeshout.gamecommon;

public enum MiniGameResultType {

    WINNER,
    UNDECIDED,
    LOSER,
    ;

    public static MiniGameResultType of(int playerCount, int rank) {
        if (isWinner(playerCount, rank)) {
            return WINNER;
        }
        if (isUndecided(playerCount, rank)) {
            return UNDECIDED;
        }
        return LOSER;
    }

    private static boolean isWinner(int playerCount, int rank) {
        return rank <= calculateWinnerCount(playerCount);
    }

    private static boolean isUndecided(int playerCount, int rank) {
        return hasUndecided(playerCount) && calculateWinnerCount(playerCount) + 1 == rank;
    }

    private static boolean hasUndecided(int playerCount) {
        return playerCount % 2 == 1;
    }

    private static int calculateWinnerCount(int playerCount) {
        return playerCount / 2;
    }
}
