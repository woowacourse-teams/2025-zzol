package coffeeshout.numberpoker.domain;

import coffeeshout.room.domain.player.Player;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NumberPokerProbabilityAdjuster {

    /** ProbabilityCalculator.ADJUSTMENT_WEIGHT 와 동일 */
    private static final double ADJUSTMENT_WEIGHT = 0.7;
    private static final int TOTAL_PROBABILITY = 10000;

    private final double stage1FoldMultiplier;
    private final double stage2FoldMultiplier;

    public NumberPokerProbabilityAdjuster(double stage1FoldMultiplier, double stage2FoldMultiplier) {
        this.stage1FoldMultiplier = stage1FoldMultiplier;
        this.stage2FoldMultiplier = stage2FoldMultiplier;
    }

    /**
     * 라운드 결과를 바탕으로 플레이어별 확률 변동량을 계산한다.
     * step은 ProbabilityCalculator.computeAdjustmentStep 과 동일한 공식으로 산출한다:
     * step = (TOTAL / playerCount) / roundCount / (playerCount / 2) * ADJUSTMENT_WEIGHT
     * 실제 적용(하한선 보정)은 Application Layer에서 담당한다.
     *
     * @param results     플레이어별 라운드 결과
     * @param playerCount 참여 플레이어 수
     * @param roundCount  총 라운드 수
     * @return 플레이어별 확률 변동량 (양수=증가, 음수=감소)
     */
    public Map<Player, Integer> calculate(Map<Player, PokerRoundResult> results, int playerCount, int roundCount) {
        final int step = computeStep(playerCount, roundCount);
        final Map<PokerRoundResult, List<Player>> byResult = groupByResult(results);
        final Set<Player> absorbers = findAbsorbers(byResult);

        if (absorbers.isEmpty()) {
            return results.keySet().stream().collect(Collectors.toMap(p -> p, p -> 0));
        }

        final Map<Player, Integer> changes = new HashMap<>();
        int totalIncrease = 0;

        for (Map.Entry<Player, PokerRoundResult> entry : results.entrySet()) {
            final Player player = entry.getKey();
            final PokerRoundResult result = entry.getValue();

            if (absorbers.contains(player)) {
                continue;
            }

            final int increase = switch (result) {
                case STAGE_1_FOLD -> (int) (step * stage1FoldMultiplier);
                case STAGE_2_FOLD -> (int) (step * stage2FoldMultiplier);
                case LOSE -> step;
                case TIE -> 0; // WIN 존재 시 비흡수자 TIE는 변동 없음
                default -> 0;
            };

            changes.put(player, increase);
            totalIncrease += increase;
        }

        final int absorptionEach = totalIncrease > 0 ? totalIncrease / absorbers.size() : 0;

        for (Player absorber : absorbers) {
            changes.put(absorber, -absorptionEach);
        }

        return changes;
    }

    private int computeStep(int playerCount, int roundCount) {
        return (int) ((TOTAL_PROBABILITY / (double) playerCount) / roundCount / (playerCount / 2.0) * ADJUSTMENT_WEIGHT);
    }

    private Map<PokerRoundResult, List<Player>> groupByResult(Map<Player, PokerRoundResult> results) {
        return results.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
    }

    private Set<Player> findAbsorbers(Map<PokerRoundResult, List<Player>> byResult) {
        final boolean hasWin = byResult.containsKey(PokerRoundResult.WIN);
        final boolean hasTie = byResult.containsKey(PokerRoundResult.TIE);
        final boolean hasLose = byResult.containsKey(PokerRoundResult.LOSE);
        final boolean hasStage1Fold = byResult.containsKey(PokerRoundResult.STAGE_1_FOLD);
        final boolean hasStage2Fold = byResult.containsKey(PokerRoundResult.STAGE_2_FOLD);

        // 1순위: WIN
        if (hasWin) {
            return Set.copyOf(byResult.get(PokerRoundResult.WIN));
        }

        // 2순위: TIE (WIN 없을 때)
        if (hasTie) {
            return Set.copyOf(byResult.get(PokerRoundResult.TIE));
        }

        // 3순위: FOLD 전체 (WIN·TIE 없고 LOSE 있을 때)
        if (hasLose) {
            final Set<Player> foldAbsorbers = new HashSet<>();
            if (hasStage1Fold) {
                foldAbsorbers.addAll(byResult.get(PokerRoundResult.STAGE_1_FOLD));
            }
            if (hasStage2Fold) {
                foldAbsorbers.addAll(byResult.get(PokerRoundResult.STAGE_2_FOLD));
            }
            return foldAbsorbers;
        }

        // 4순위: STAGE_1_FOLD (WIN·TIE·LOSE 없고 STAGE_2_FOLD 있을 때)
        if (hasStage1Fold && hasStage2Fold) {
            return Set.copyOf(byResult.get(PokerRoundResult.STAGE_1_FOLD));
        }

        return Set.of();
    }
}
