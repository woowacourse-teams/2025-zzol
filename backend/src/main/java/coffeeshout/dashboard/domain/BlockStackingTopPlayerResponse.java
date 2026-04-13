package coffeeshout.dashboard.domain;

public record BlockStackingTopPlayerResponse(
        String playerName,
        Long maxFloor
) {
}
