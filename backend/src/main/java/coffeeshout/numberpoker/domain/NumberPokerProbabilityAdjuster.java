package coffeeshout.numberpoker.domain;

import coffeeshout.room.domain.player.Player;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NumberPokerProbabilityAdjuster {

    private static final int TOTAL_PROBABILITY = 10000;

    private final double stage1FoldMultiplier;
    private final double stage2FoldMultiplier;

    public NumberPokerProbabilityAdjuster(double stage1FoldMultiplier, double stage2FoldMultiplier) {
        this.stage1FoldMultiplier = stage1FoldMultiplier;
        this.stage2FoldMultiplier = stage2FoldMultiplier;
    }

    /**
     * 라운드 결과를 바탕으로 플레이어별 확률 변동량을 계산한다.
     * 실제 적용(하한선 보정)은 Application Layer에서 담당한다.
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
            if (absorbers.contains(player)) {
                continue;
            }
            final int increase = increaseFor(entry.getValue(), step);
            changes.put(player, increase);
            totalIncrease += increase;
        }

        distributeAbsorption(absorbers, totalIncrease, changes);
        return changes;
    }

    // ── Step 계산 ─────────────────────────────────────────────────────────────

    /**
     * step = (총확률 / 플레이어 수) / 라운드 수 / 경쟁 포지션 수
     *
     * <p>경쟁 포지션 수(playerCount / 2): 딜러 vs 플레이어 포커에서 한 라운드당 대략
     * 절반의 플레이어가 승부 포지션에 있다는 가정. 이 값을 나누지 않으면 다수가 동시에
     * 패배해 단일 흡수자가 받는 변동량이 플레이어 수에 비례해 폭발적으로 증가한다.
     */
    private int computeStep(int playerCount, int roundCount) {
        final double competitivePositions = playerCount / 2.0;
        return (int) (TOTAL_PROBABILITY / (double) playerCount / roundCount / competitivePositions);
    }

    private int increaseFor(PokerRoundResult result, int step) {
        return switch (result) {
            case STAGE_1_FOLD -> (int) (step * stage1FoldMultiplier);
            case STAGE_2_FOLD -> (int) (step * stage2FoldMultiplier);
            case LOSE -> step;
            default -> 0;
        };
    }

    // ── 흡수 분배 ─────────────────────────────────────────────────────────────

    /**
     * 총 증가분을 흡수자에게 균등 배분한다.
     * 정수 나눗셈 나머지는 이름 순 마지막 흡수자에게 배분해 zero-sum을 보장한다.
     */
    private void distributeAbsorption(Set<Player> absorbers, int totalIncrease, Map<Player, Integer> changes) {
        if (totalIncrease == 0) {
            absorbers.forEach(absorber -> changes.put(absorber, 0));
            return;
        }
        final List<Player> sorted = absorbers.stream()
                .sorted(Comparator.comparing(p -> p.getName().value()))
                .toList();
        final int shareEach = totalIncrease / sorted.size();
        int remaining = totalIncrease;
        for (int i = 0; i < sorted.size() - 1; i++) {
            changes.put(sorted.get(i), -shareEach);
            remaining -= shareEach;
        }
        changes.put(sorted.getLast(), -remaining);
    }

    // ── 흡수자 선택 ───────────────────────────────────────────────────────────

    /**
     * 흡수자 우선순위: WIN > TIE > FOLD 전체(LOSE 있을 때) > STAGE_1_FOLD(STAGE_2_FOLD만 있을 때)
     * 흡수자가 없으면 빈 집합을 반환한다.
     */
    private Set<Player> findAbsorbers(Map<PokerRoundResult, List<Player>> byResult) {
        if (byResult.containsKey(PokerRoundResult.WIN)) {
            return Set.copyOf(byResult.get(PokerRoundResult.WIN));
        }
        if (byResult.containsKey(PokerRoundResult.TIE)) {
            return Set.copyOf(byResult.get(PokerRoundResult.TIE));
        }
        if (byResult.containsKey(PokerRoundResult.LOSE)) {
            return collectFoldAbsorbers(byResult);
        }
        if (byResult.containsKey(PokerRoundResult.STAGE_1_FOLD)
                && byResult.containsKey(PokerRoundResult.STAGE_2_FOLD)) {
            return Set.copyOf(byResult.get(PokerRoundResult.STAGE_1_FOLD));
        }
        return Set.of();
    }

    /** LOSE 있을 때 STAGE_1_FOLD + STAGE_2_FOLD 전체를 흡수자로 수집한다. */
    private Set<Player> collectFoldAbsorbers(Map<PokerRoundResult, List<Player>> byResult) {
        final Set<Player> foldAbsorbers = new HashSet<>();
        for (PokerRoundResult foldResult : List.of(PokerRoundResult.STAGE_1_FOLD, PokerRoundResult.STAGE_2_FOLD)) {
            if (byResult.containsKey(foldResult)) {
                foldAbsorbers.addAll(byResult.get(foldResult));
            }
        }
        return foldAbsorbers;
    }

    private Map<PokerRoundResult, List<Player>> groupByResult(Map<Player, PokerRoundResult> results) {
        return results.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
    }
}
