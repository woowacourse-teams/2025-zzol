package coffeeshout.numberpoker.domain;

import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.roulette.Probability;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NumberPokerProbabilityAdjuster {

    private final double stage1FoldMultiplier;
    private final double stage2FoldMultiplier;

    public NumberPokerProbabilityAdjuster(double stage1FoldMultiplier, double stage2FoldMultiplier) {
        this.stage1FoldMultiplier = stage1FoldMultiplier;
        this.stage2FoldMultiplier = stage2FoldMultiplier;
    }

    /**
     * 라운드 결과를 바탕으로 플레이어별 확률 변동량을 계산한다.
     * 실제 적용(하한선 보정)은 Application Layer에서 담당한다.
     *
     * <p>handRankings: 폴드하지 않은 플레이어의 HandRanking 맵.
     * WIN 흡수자가 여럿일 때 핸드 강도에 따라 흡수량을 차등 배분하는 데 사용한다.
     * WIN 이외의 흡수자 그룹(TIE·FOLD)에는 균등 배분이 적용된다.
     *
     * <p>currentRoundNumber: 라운드 번호(1-indexed). 후반 라운드일수록 step이 커진다.
     */
    public Map<Player, Integer> calculate(
            Map<Player, PokerRoundResult> results,
            Map<Player, HandRanking> handRankings,
            int playerCount,
            int roundCount,
            int currentRoundNumber) {
        final int step = computeStep(playerCount, roundCount, currentRoundNumber);
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

        final boolean absorbersAreWinners = byResult.containsKey(PokerRoundResult.WIN)
                && new HashSet<>(byResult.get(PokerRoundResult.WIN)).equals(absorbers);
        distributeAbsorption(absorbers, totalIncrease, changes, handRankings, absorbersAreWinners);
        return changes;
    }

    // ── Step 계산 ─────────────────────────────────────────────────────────────

    /**
     * step = base × roundMultiplier
     *
     * <p>base = 총확률 / 플레이어 수 / 라운드 수
     * (기존 경쟁포지션 수 나누기 제거 — 플레이어 수에 따른 변동폭 과소 억제 문제 해소)
     *
     * <p>roundMultiplier = 2 × currentRoundNumber / (roundCount + 1)
     * → 라운드 번호에 비례해 선형 증가. 전체 라운드 평균은 1.0을 유지하므로
     * 총 기댓값은 동일하고, 후반 라운드일수록 개별 변동폭이 커진다.
     * 변동 없는 라운드의 weight가 이월될 경우 currentRoundNumber가 totalRounds를
     * 초과할 수 있으며, 이는 의도적으로 허용된 긴장 고조 효과다.
     */
    private int computeStep(int playerCount, int roundCount, int currentRoundNumber) {
        final int baseStep = (int) (Probability.TOTAL.value() / (double) playerCount / roundCount);
        final double roundMultiplier = 2.0 * currentRoundNumber / (roundCount + 1);
        return (int) (baseStep * roundMultiplier);
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

    private void distributeAbsorption(
            Set<Player> absorbers,
            int totalIncrease,
            Map<Player, Integer> changes,
            Map<Player, HandRanking> handRankings,
            boolean absorbersAreWinners) {
        if (totalIncrease == 0) {
            absorbers.forEach(absorber -> changes.put(absorber, 0));
            return;
        }
        if (absorbersAreWinners && absorbers.size() > 1
                && absorbers.stream().allMatch(handRankings::containsKey)) {
            distributeByRanking(absorbers, totalIncrease, changes, handRankings);
        } else {
            distributeEvenly(absorbers, totalIncrease, changes);
        }
    }

    /**
     * WIN 흡수자들에게 핸드 랭킹 순위 비례로 흡수량을 배분한다.
     * 순위 1위(최강 핸드)가 weight n, 2위가 n-1, ..., n위가 1을 가진다.
     * 동점 핸드가 있는 경우 이름 순으로 순위를 결정한다.
     * 마지막 흡수자에게 나머지를 부여해 zero-sum을 보장한다.
     */
    private void distributeByRanking(
            Set<Player> absorbers,
            int totalIncrease,
            Map<Player, Integer> changes,
            Map<Player, HandRanking> handRankings) {
        final List<Player> sorted = absorbers.stream()
                .sorted(Comparator.comparing((Player p) -> handRankings.get(p)).reversed()
                        .thenComparing(p -> p.getName().value()))
                .toList();
        final int n = sorted.size();
        final int totalWeight = n * (n + 1) / 2;

        int distributed = 0;
        for (int i = 0; i < n - 1; i++) {
            final int weight = n - i;
            final int share = totalIncrease * weight / totalWeight;
            changes.put(sorted.get(i), -share);
            distributed += share;
        }
        changes.put(sorted.getLast(), -(totalIncrease - distributed));
    }

    /**
     * 흡수자들에게 총 증가분을 균등 배분한다.
     * 정수 나눗셈 나머지는 이름 순 마지막 흡수자에게 부여해 zero-sum을 보장한다.
     */
    private void distributeEvenly(Set<Player> absorbers, int totalIncrease, Map<Player, Integer> changes) {
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
