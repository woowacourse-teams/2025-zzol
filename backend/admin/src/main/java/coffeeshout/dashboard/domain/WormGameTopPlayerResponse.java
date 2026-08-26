package coffeeshout.dashboard.domain;

public record WormGameTopPlayerResponse(
        String playerName,
        Long bestSurvivalMillis
) {
}
