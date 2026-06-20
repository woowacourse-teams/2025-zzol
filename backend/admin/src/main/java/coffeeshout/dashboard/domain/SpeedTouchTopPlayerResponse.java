package coffeeshout.dashboard.domain;

public record SpeedTouchTopPlayerResponse(
        String playerName,
        long bestTime
) {
}
