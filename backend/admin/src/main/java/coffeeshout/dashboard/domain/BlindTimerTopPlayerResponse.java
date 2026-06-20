package coffeeshout.dashboard.domain;

public record BlindTimerTopPlayerResponse(
        String playerName,
        long bestErrorMillis
) {
}
