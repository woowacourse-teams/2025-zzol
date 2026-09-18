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
     * @param games          레이싱·블록 쌓기·1 to 25·초시계 순서로 항상 네 개
     */
    record MiniGameRecords(int totalPlayCount, MostPlayed mostPlayed, List<GameRecord> games) {}

    record MostPlayed(MiniGameType type, int playCount) {}

    /** 완주 기록만 센다. 값은 저장 단위(ms, 층) 그대로이고 기록이 없으면 best·average는 null이다. */
    record GameRecord(MiniGameType type, int playCount, Long best, Long average) {}
}
