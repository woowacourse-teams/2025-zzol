package coffeeshout.minigame.domain;

import coffeeshout.gamecommon.PlayerView;
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

    private final Map<PlayerView, Integer> rank;

    public MiniGameResult(Map<PlayerView, Integer> rank) {
        this.rank = rank;
    }

    public Integer getPlayerRank(PlayerView player) {
        return rank.get(player);
    }

    public static MiniGameResult fromDescending(@NonNull Map<? extends PlayerView, MiniGameScore> playerScores) {
        return of(playerScores, Comparator.reverseOrder());
    }

    public static MiniGameResult fromAscending(@NonNull Map<? extends PlayerView, MiniGameScore> playerScores) {
        return of(playerScores, Comparator.naturalOrder());
    }

    public static MiniGameResult of(
            @NonNull Map<? extends PlayerView, MiniGameScore> playerScores,
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
            if (prevScore == null || !score.equals(prevScore)) {
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
