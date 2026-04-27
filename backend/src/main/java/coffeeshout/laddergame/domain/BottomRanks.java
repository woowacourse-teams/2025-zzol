package coffeeshout.laddergame.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class BottomRanks {

    private final Map<Integer, Integer> ranks;

    private BottomRanks(Map<Integer, Integer> ranks) {
        this.ranks = Map.copyOf(ranks);
    }

    public static BottomRanks generate(int n) {
        final List<Integer> rankList = new ArrayList<>(IntStream.rangeClosed(1, n).boxed().toList());
        Collections.shuffle(rankList);
        final Map<Integer, Integer> ranks = new HashMap<>();
        for (int i = 0; i < n; i++) {
            ranks.put(i, rankList.get(i));
        }
        return new BottomRanks(ranks);
    }

    public int getRank(int poleIndex) {
        return ranks.getOrDefault(poleIndex, 0);
    }

    public Map<Integer, Integer> getAll() {
        return ranks;
    }
}
