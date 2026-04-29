package coffeeshout.laddergame.domain;

import coffeeshout.global.exception.custom.BusinessException;
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
        if (n <= 0) {
            throw new BusinessException(LadderGameErrorCode.INVALID_PLAYER_COUNT, "n must be > 0");
        }
        final List<Integer> rankList = new ArrayList<>(IntStream.rangeClosed(1, n).boxed().toList());
        Collections.shuffle(rankList);
        final Map<Integer, Integer> ranks = new HashMap<>();
        for (int i = 0; i < n; i++) {
            ranks.put(i, rankList.get(i));
        }
        return new BottomRanks(ranks);
    }

    public int getRank(int poleIndex) {
        if (!ranks.containsKey(poleIndex)) {
            throw new BusinessException(LadderGameErrorCode.INVALID_POLE_INDEX,
                    "유효하지 않은 기둥 인덱스입니다: " + poleIndex);
        }
        return ranks.get(poleIndex);
    }

    public Map<Integer, Integer> getAll() {
        return ranks;
    }
}
