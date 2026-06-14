package coffeeshout.dashboard.domain;

public record RacingGameTopPlayerResponse(
        String playerName,
        long bestTime
) {
}
