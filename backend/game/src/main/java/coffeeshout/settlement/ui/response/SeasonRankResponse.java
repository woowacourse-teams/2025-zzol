package coffeeshout.settlement.ui.response;

/** 특정 회원(userCode)의 시즌 순위 응답. */
public record SeasonRankResponse(
        String seasonKey,
        String nickname,
        String userCode,
        int rank,
        long totalPoints,
        String tier,
        long totalMembers
) {
}
