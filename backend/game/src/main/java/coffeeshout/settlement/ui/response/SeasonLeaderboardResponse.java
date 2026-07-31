package coffeeshout.settlement.ui.response;

import java.util.List;

/**
 * 시즌 리더보드 응답. 회원 식별은 ADR-0024의 전역 식별({@code nickname#userCode})을 따르고
 * 내부 userId는 노출하지 않는다.
 */
public record SeasonLeaderboardResponse(String seasonKey, long totalMembers, List<Row> rows) {

    public record Row(int rank, String nickname, String userCode, long totalPoints, String tier) {
    }
}
