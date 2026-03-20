package coffeeshout.dashboard.domain;

public record TopWinnerResponse(
        String playerName,
        Long winCount
) {
}
