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

    private final Integer playerCount;
    private final Integer roundCount;
    private final double adjustmentWeight;
    private final Map<Integer, Integer> probabilityChangeRangeMap;

    public ProbabilityCalculator(Integer playerCount, Integer roundCount, double adjustmentWeight) {
        validate(playerCount, roundCount, adjustmentWeight);
        this.playerCount = playerCount;
        this.roundCount = roundCount;
        this.adjustmentWeight = adjustmentWeight;
        this.probabilityChangeRangeMap = processProbabilityChangeRangeMap();
    }

    public int calculateProbabilityChange(int rank, int tieCount) {
        final int sum = IntStream.range(rank, rank + tieCount)
                .map(i -> probabilityChangeRangeMap.getOrDefault(i, 0))
                .sum();
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
        return maxAdjustment.divide(countAdjustableRanks()).multiple(adjustmentWeight);
    }

    private Probability computeInitialProbability() {
        return Probability.TOTAL.divide(playerCount);
    }

    private int countAdjustableRanks() {
        return playerCount / 2;
    }

    private void validate(Integer playerCount, Integer roundCount, double adjustmentWeight) {
        isTrue(playerCount >= 2, "플레이어는 2명 이상이어야 합니다.");
        isTrue(roundCount > 0, "라운드 수는 양수여야 합니다.");
        isTrue(adjustmentWeight >= 0.1 && adjustmentWeight <= 0.9, "가중치는 0.1 이상 0.9 이하여야 합니다.");
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
