package coffeeshout.gamecommon;

/**
 * 내 기록 화면용 회원 룰렛 통계 조회 포트(#1794). {@code :user}가 호출하고 {@code :room}이 구현한다 —
 * {@link RoomSnapshotQuery}(ADR-0034)와 같은 역전 패턴으로 {@code :user→:room} 의존을 만들지 않는다.
 */
public interface MemberRouletteRecordQuery {

    RouletteRecord findByUserId(long userId);

    /**
     * 회원이 참여한 방의 룰렛 결과 집계.
     *
     * @param winCount       당첨 횟수
     * @param survivalStreak 마지막 당첨 이후 피한 횟수. 당첨이 없으면 참여 판수와 같다
     * @param playCount      참여 판수
     */
    record RouletteRecord(int winCount, int survivalStreak, int playCount) {}
}
