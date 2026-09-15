package coffeeshout.admin.overview.domain;

import coffeeshout.minigame.domain.MiniGameType;

/**
 * 게임별 플레이 집계.
 *
 * <p>한때 <b>완료된 판만</b> 셌다. "mini_game_play 행은 게임이 끝날 때 쌓인다"고 적혀
 * 있었는데 사실이 아니다. 그 행은 {@code GameStartReadyEvent} 를 받는 시점, 즉 게임이
 * <b>시작될 때</b> 쌓인다({@code MiniGamePersistenceService}). 결과({@code mini_game_result})
 * 만 끝날 때 쌓인다.
 *
 * <p>그래서 둘의 차이가 곧 <b>중간에 깨진 판</b>이다. 어느 게임에서 사람이 빠져나가는지는
 * 이 차이로만 보인다. 완료 판만 세던 때는 그 게임이 인기가 없는 것인지 시작은 하는데
 * 끝까지 못 가는 것인지 구분할 방법이 없었다.
 *
 * <p>깨진 판이 전부 사람이 나간 것은 아니다. 결과 저장이 실패해도 여기로 잡힌다. 그래도
 * 둘 다 <b>봐야 하는 사건</b>이라 나누지 않는다.
 *
 * @param started  시작한 판
 * @param finished 그중 결과가 남은 판
 * @param share    전체 시작 판 대비 비중. 0.0 ~ 1.0
 */
public record GamePlayStat(MiniGameType miniGameType, long started, long finished, double share) {

    /** 이탈한 판. 시작은 했는데 결과가 한 줄도 안 남은 판이다. */
    public long dropped() {
        return started - finished;
    }
}
