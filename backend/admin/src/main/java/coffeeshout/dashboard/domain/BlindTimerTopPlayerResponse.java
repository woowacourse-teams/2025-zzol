package coffeeshout.dashboard.domain;

public record BlindTimerTopPlayerResponse(String playerName, long bestErrorMillis, boolean masked) {

    public BlindTimerTopPlayerResponse(String playerName, long bestErrorMillis) {
        this(playerName, bestErrorMillis, false);
    }

    public BlindTimerTopPlayerResponse mask() {
        return new BlindTimerTopPlayerResponse(MaskedNickname.TEXT, bestErrorMillis, true);
    }
}
