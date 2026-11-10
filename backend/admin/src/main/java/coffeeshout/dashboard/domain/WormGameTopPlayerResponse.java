package coffeeshout.dashboard.domain;

public record WormGameTopPlayerResponse(String playerName, Long bestSurvivalMillis, boolean masked) {

    public WormGameTopPlayerResponse(String playerName, Long bestSurvivalMillis) {
        this(playerName, bestSurvivalMillis, false);
    }

    public WormGameTopPlayerResponse mask() {
        return new WormGameTopPlayerResponse(MaskedNickname.TEXT, bestSurvivalMillis, true);
    }
}
