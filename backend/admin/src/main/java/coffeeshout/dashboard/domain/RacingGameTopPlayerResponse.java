package coffeeshout.dashboard.domain;

public record RacingGameTopPlayerResponse(String playerName, long bestTime, boolean masked) {

    public RacingGameTopPlayerResponse(String playerName, long bestTime) {
        this(playerName, bestTime, false);
    }

    public RacingGameTopPlayerResponse mask() {
        return new RacingGameTopPlayerResponse(MaskedNickname.TEXT, bestTime, true);
    }
}
