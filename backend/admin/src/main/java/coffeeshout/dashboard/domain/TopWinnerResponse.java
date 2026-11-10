package coffeeshout.dashboard.domain;

public record TopWinnerResponse(String nickname, String userCode, Long winCount, boolean masked) {

    public TopWinnerResponse(String nickname, String userCode, Long winCount) {
        this(nickname, userCode, winCount, false);
    }

    public TopWinnerResponse mask() {
        return new TopWinnerResponse(MaskedNickname.TEXT, userCode, winCount, true);
    }
}
