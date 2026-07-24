package coffeeshout.settlement.domain;

/**
 * 방 내 순위를 시즌 포인트로 환산한다. 게임 종류와 무관한 순위 기반 정책이라
 * 정산 대상 게임이 늘어나도 그대로 재사용한다(#1610).
 * <p>
 * 참가만 해도 기본 포인트를 주는 이유는 시즌 랭킹이 "잘하는 사람"뿐 아니라
 * "많이 하는 사람"에게도 동기가 되도록 하기 위해서다.
 */
public final class SeasonPointPolicy {

    private static final int FIRST_PLACE = 100;
    private static final int SECOND_PLACE = 70;
    private static final int THIRD_PLACE = 50;
    private static final int PARTICIPATION = 30;

    private SeasonPointPolicy() {
    }

    public static int pointsFor(int rank) {
        if (rank < 1) {
            throw new IllegalArgumentException("순위는 1 이상이어야 합니다: " + rank);
        }
        return switch (rank) {
            case 1 -> FIRST_PLACE;
            case 2 -> SECOND_PLACE;
            case 3 -> THIRD_PLACE;
            default -> PARTICIPATION;
        };
    }
}
