package coffeeshout.gamecommon;

import coffeeshout.minigame.domain.MiniGameType;
import java.util.List;

/**
 * 내 기록 화면용 회원 미니게임 기록 조회 포트(#1794). {@code :user}가 호출하고 {@code :game}이 구현한다 —
 * {@link RoomSnapshotQuery}(ADR-0034)와 같은 역전 패턴으로 {@code :user→:game} 의존을 만들지 않는다.
 */
public interface MemberMiniGameRecordQuery {

    MiniGameRecords findByUserId(long userId);

    /**
     * @param totalPlayCount 8개 게임 전체, 미완주 포함 판수
     * @param mostPlayed     최다 게임. 판이 없으면 null
     * @param games          레이싱·블록 쌓기·초시계·1 to 25 순서로 항상 네 개
     */
    record MiniGameRecords(int totalPlayCount, MostPlayed mostPlayed, List<GameRecord> games) {}

    record MostPlayed(MiniGameType type, int playCount) {}

    /**
     * 완주 기록만 센다. 값은 저장 단위(ms, 층) 그대로다. 전체 평균과 상위 %의 모집단은 완주 기록이 있는 로그인 회원이고
     * 게스트 행은 들어가지 않는다.
     *
     * @param best          기록이 없으면 null
     * @param average       내 완주 기록 평균. 기록이 없으면 null
     * @param globalAverage 회원 전체의 완주 기록을 판수로 가중한 평균. 완주한 회원이 없으면 null
     * @param percentile    내 최고 기록이 회원별 최고 기록 중 몇 등인지를 상위 %로 센 값. 랭킹 탭과 같은 기준이라 많이 할수록
     *                      손해가 없다. 동률은 같은 등수. 내 기록이 없으면 null
     * @param memberCount   완주 기록이 있는 회원 수
     */
    record GameRecord(
            MiniGameType type,
            int playCount,
            Long best,
            Long average,
            Long globalAverage,
            Integer percentile,
            int memberCount) {}
}
