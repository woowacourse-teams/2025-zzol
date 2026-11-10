package coffeeshout.dashboard.domain;

public record SpeedTouchTopPlayerResponse(String playerName, long bestTime, boolean masked) {

    public SpeedTouchTopPlayerResponse(String playerName, long bestTime) {
        this(playerName, bestTime, false);
    }

    public SpeedTouchTopPlayerResponse mask() {
        return new SpeedTouchTopPlayerResponse(MaskedNickname.TEXT, bestTime, true);
    }
}
