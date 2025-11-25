package coffeeshout.room.domain.roulette;

import static org.springframework.util.Assert.isTrue;

import coffeeshout.minigame.domain.MiniGameResultType;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
    등수별 확률 조정 정도를 계산하는 클래스
 */
public class ProbabilityCalculator {

    protected static final double ADJUSTMENT_WEIGHT = 0.7;

    private final Integer playerCount;
    private final Integer roundCount;
    private final Map<Integer, Integer> probabilityChangeRangeMap;

    public ProbabilityCalculator(Integer playerCount, Integer roundCount) {
        validate(playerCount, roundCount);
        this.playerCount = playerCount;
        this.roundCount = roundCount;
        this.probabilityChangeRangeMap = processProbabilityChangeRangeMap();
    }

    public int calculateProbabilityChange(int rank, int tieCount) {
        int sum = 0;
        for (int i = rank; i < rank + tieCount; i++) {
            Integer probabilityChange = probabilityChangeRangeMap.getOrDefault(i, 0);
            sum += probabilityChange;
        }
        return sum / tieCount;
    }

    private int relativeRank(int rank) {
        if (rank <= countAdjustableRanks()) {
            return rank;
        }
        return playerCount - rank + 1;
    }

    private Probability computeAdjustmentStep() {
        final Probability maxAdjustment = computeInitialProbability().divide(roundCount);
        return maxAdjustment.divide(countAdjustableRanks()).multiple(ADJUSTMENT_WEIGHT);
    }

    private Probability computeInitialProbability() {
        return Probability.TOTAL.divide(playerCount);
    }

    private int countAdjustableRanks() {
        return playerCount / 2;
    }

    private void validate(Integer playerCount, Integer roundCount) {
        isTrue(playerCount >= 2, "플레이어는 2명 이상이어야 합니다.");
        isTrue(roundCount > 0, "라운드 수는 양수여야 합니다.");
    }

    private Map<Integer, Integer> processProbabilityChangeRangeMap() {
        return IntStream.rangeClosed(1, playerCount)
                .boxed()
                .collect(Collectors.toMap(
                        rank -> rank,
                        this::processProbabilityChange
                ));
    }


    private Integer processProbabilityChange(int rank) {
        final MiniGameResultType resultType = MiniGameResultType.of(playerCount, rank);
        final Probability probability = resultType.adjustProbability(countAdjustableRanks(), relativeRank(rank),
                computeAdjustmentStep());

        return probability.getProbabilityChange(resultType);
    }
}
