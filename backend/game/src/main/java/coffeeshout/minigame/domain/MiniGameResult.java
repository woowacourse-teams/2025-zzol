package coffeeshout.minigame.domain;

import coffeeshout.room.domain.player.PlayerName;
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

    private final Map<PlayerName, Integer> rank;

    public MiniGameResult(Map<PlayerName, Integer> rank) {
        this.rank = rank;
    }

    public Integer getPlayerRank(PlayerName playerName) {
        return rank.get(playerName);
    }

    public static MiniGameResult fromDescending(@NonNull Map<PlayerName, MiniGameScore> playerScores) {
        return of(playerScores, Comparator.reverseOrder());
    }

    public static MiniGameResult fromAscending(@NonNull Map<PlayerName, MiniGameScore> playerScores) {
        return of(playerScores, Comparator.naturalOrder());
    }

    public static MiniGameResult of(
            @NonNull Map<PlayerName, MiniGameScore> playerScores,
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
            if (!isTieScore(score, prevScore)) {
                rank = count;
                prevScore = score;
            }
            ranks.put(score, rank);
        }
        return ranks;
    }

    private static boolean isTieScore(MiniGameScore score, MiniGameScore prevScore) {
        return score.equals(prevScore);
    }

    public int getTieCountByRank(int rank) {
        return (int) this.rank.values()
                .stream()
                .filter(value -> value.equals(rank))
                .count();
    }

    public Map<PlayerName, Integer> toRankMap() {
        return Map.copyOf(rank);
    }
}
