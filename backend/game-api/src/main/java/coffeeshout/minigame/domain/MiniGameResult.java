package coffeeshout.minigame.domain;

import coffeeshout.gamecommon.Gamer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class MiniGameResult {

    private final Map<Gamer, Integer> rank;

    public MiniGameResult(Map<Gamer, Integer> rank) {
        this.rank = rank;
    }

    public Integer getPlayerRank(Gamer gamer) {
        return rank.get(gamer);
    }

    /**
     * 순위 맵을 이름 기준으로 변환한다. {@code MiniGameFinishedEvent}가 원시 타입만 운반하도록
     * 6개 게임의 중복 변환을 막는 단일 변환 지점이다(ADR-0023 결정 5).
     */
    public Map<String, Integer> toRankMap() {
        return rank.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getName(), Entry::getValue));
    }

    public static MiniGameResult fromDescending(@NonNull Map<Gamer, MiniGameScore> playerScores) {
        return of(playerScores, Comparator.reverseOrder());
    }

    public static MiniGameResult fromAscending(@NonNull Map<Gamer, MiniGameScore> playerScores) {
        return of(playerScores, Comparator.naturalOrder());
    }

    public static MiniGameResult of(
            @NonNull Map<Gamer, MiniGameScore> playerScores,
            Comparator<MiniGameScore> comparator
    ) {
        final List<MiniGameScore> sortedScores = playerScores.values().stream()
                .sorted(comparator)
                .toList();
        final Map<MiniGameScore, Integer> ranks = calculateRank(sortedScores);
        return new MiniGameResult(playerScores.entrySet().stream().collect(Collectors.toMap(
                Entry::getKey,
                entry -> ranks.get(entry.getValue())
        )));
    }

    private static Map<MiniGameScore, Integer> calculateRank(List<MiniGameScore> sortedScores) {
        final Map<MiniGameScore, Integer> ranks = new HashMap<>();
        int rank = 1;
        int count = 0;
        MiniGameScore prevScore = null;
        for (MiniGameScore score : sortedScores) {
            count++;
            if (!score.equals(prevScore)) {
                rank = count;
                prevScore = score;
            }
            ranks.put(score, rank);
        }
        return ranks;
    }

    public int getTieCountByRank(int rank) {
        return (int) this.rank.values()
                .stream()
                .filter(value -> value.equals(rank))
                .count();
    }
}
