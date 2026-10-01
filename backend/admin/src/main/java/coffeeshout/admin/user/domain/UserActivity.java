package coffeeshout.admin.user.domain;

/**
 * 유저의 참여 이력 요약.
 *
 * @param roomCount 참여한 방 수
 * @param winCount  룰렛에 걸린 횟수
 */
public record UserActivity(long roomCount, long winCount) {

    public static UserActivity empty() {
        return new UserActivity(0, 0);
    }

    /** 참여 대비 당첨 비율. 0.0 ~ 1.0. 참여가 없으면 0. */
    public double winRate() {
        return roomCount == 0 ? 0 : (double) winCount / roomCount;
    }
}
