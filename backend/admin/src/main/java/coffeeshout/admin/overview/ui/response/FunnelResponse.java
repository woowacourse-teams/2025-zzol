package coffeeshout.admin.overview.ui.response;

import coffeeshout.admin.overview.domain.RoomFunnel;

/**
 * 방 진행 퍼널. 단계 이름은 화면 라벨과 1:1로 맞춘다.
 *
 * <p>"2인 이상 입장" 단계는 뺐다. 2인부터 게임이 되므로 늘 100%였다.
 * 그 자리에 "미니게임 완료"를 넣었다 - 게임 시작과의 차이가 곧 하다가 나간 방이다.
 *
 * <p>SCORE_BOARD 단계는 없다. 그 상태는 DB로 내려가지 않아 언제나 0이 찍히는데,
 * 0이 늘 찍히는 칸이 표에 있으면 지표 전체를 못 믿게 된다.
 */
public record FunnelResponse(
        long created,
        long gameStarted,
        long miniGamePlayed,
        long rouletteReached,
        long completed,
        double completionRate) {

    public static FunnelResponse from(RoomFunnel funnel) {
        return new FunnelResponse(
                funnel.created(),
                funnel.gameStarted(),
                funnel.miniGamePlayed(),
                funnel.rouletteReached(),
                funnel.completed(),
                funnel.completionRate());
    }
}
