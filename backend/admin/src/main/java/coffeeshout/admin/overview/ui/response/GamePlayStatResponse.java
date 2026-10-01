package coffeeshout.admin.overview.ui.response;

import coffeeshout.admin.overview.domain.GamePlayStat;
import coffeeshout.minigame.domain.MiniGameType;

/**
 * @param label    화면에 그대로 찍는 한글 이름. {@link MiniGameType} 이 이미 들고 있는 값이다.
 *                 <p>프론트가 enum 이름과 한글 이름의 대응표를 따로 들고 있었는데, 게임이
 *                 늘어나자 빠진 항목이 영문 그대로 노출됐다(LADDER_GAME). 같은 사실을 두 곳에
 *                 적어 두면 한쪽만 고치는 날이 온다. 서버가 가진 것을 그대로 내려보낸다.
 * @param started  시작한 판. {@code mini_game_play} 행은 게임이 시작될 때 쌓인다
 * @param finished 그중 결과가 남은 판. {@code started} 와의 차이가 중간에 깨진 판이다
 * @param share    전체 시작 판 대비 비중. 0.0 ~ 1.0
 */
public record GamePlayStatResponse(MiniGameType miniGameType, String label, long started, long finished, double share) {

    public static GamePlayStatResponse from(GamePlayStat stat) {
        return new GamePlayStatResponse(
                stat.miniGameType(), stat.miniGameType().label, stat.started(), stat.finished(), stat.share());
    }
}
