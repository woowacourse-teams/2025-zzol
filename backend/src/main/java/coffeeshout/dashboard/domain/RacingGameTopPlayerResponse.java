package coffeeshout.dashboard.domain;

public record RacingGameTopPlayerResponse(
        String playerName,
        double avgRank,
        long totalScore
) {
}
