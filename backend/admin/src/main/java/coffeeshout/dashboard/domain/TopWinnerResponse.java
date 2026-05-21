package coffeeshout.dashboard.domain;

public record TopWinnerResponse(
        String nickname,
        String userCode,
        Long winCount
) {
}
