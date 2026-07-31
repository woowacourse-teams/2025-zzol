package coffeeshout.settlement.domain;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.SystemException;
import java.util.List;

/**
 * 방 내 순위를 시즌 포인트로 환산한다. 게임 종류와 무관한 순위 기반 정책이라
 * 정산 대상 게임이 늘어나도 그대로 재사용한다(#1610).
 * <p>
 * 참가만 해도 기본 포인트를 주는 이유는 시즌 랭킹이 "잘하는 사람"뿐 아니라
 * "많이 하는 사람"에게도 동기가 되도록 하기 위해서다.
 * <p>
 * 동점은 골프·F1의 관례를 따라 <b>동점 구간 포인트를 합산해 균등 분배</b>한다(리뷰 #1612).
 * 예: 2인 동점 1등 → (100+70)/2 = 85점씩. 전원 동점 판은 각자 평균 포인트를 받게 되어
 * 정상 플레이의 기대 포인트와 같아지므로, 전원 실패로 최고점을 담합하는 이득이 사라진다.
 * 분배 결과가 정수가 아니면 반올림한다.
 */
public final class SeasonPointPolicy {

    private static final int FIRST_PLACE = 100;
    private static final int SECOND_PLACE = 70;
    private static final int THIRD_PLACE = 50;
    private static final int PARTICIPATION = 30;

    private SeasonPointPolicy() {}

    /**
     * @param rank     포인트를 계산할 회원의 순위
     * @param allRanks 그 판 전체 플레이어(게스트 포함)의 순위 목록 — 동점자 수 산정에 필요하다.
     *                 게스트도 순위 한 자리를 차지하므로 회원 순위만으로는 동점 구간을 알 수 없다.
     */
    public static int pointsFor(int rank, List<Integer> allRanks) {
        if (rank < 1) {
            throw new SystemException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "순위는 1 이상이어야 합니다: " + rank);
        }
        final int tieCount = countTies(rank, allRanks);
        if (tieCount <= 1) {
            return basePointsFor(rank);
        }
        long sum = 0;
        for (int position = rank; position < rank + tieCount; position++) {
            sum += basePointsFor(position);
        }
        return Math.toIntExact(Math.round((double) sum / tieCount));
    }

    private static int countTies(int rank, List<Integer> allRanks) {
        // 구버전 이벤트(allRanks 없음)가 재전달되면 동점 정보 없이 기본표로 처리한다
        if (allRanks == null || allRanks.isEmpty()) {
            return 1;
        }
        return Math.toIntExact(
                allRanks.stream().filter(r -> r != null && r == rank).count());
    }

    private static int basePointsFor(int rank) {
        return switch (rank) {
            case 1 -> FIRST_PLACE;
            case 2 -> SECOND_PLACE;
            case 3 -> THIRD_PLACE;
            default -> PARTICIPATION;
        };
    }
}
