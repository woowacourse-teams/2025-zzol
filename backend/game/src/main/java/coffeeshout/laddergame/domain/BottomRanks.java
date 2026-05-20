package coffeeshout.laddergame.domain;

import coffeeshout.exception.custom.BusinessException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

public class BottomRanks {

    private final Map<Integer, Integer> ranks;

    private BottomRanks(Map<Integer, Integer> ranks) {
        this.ranks = Map.copyOf(ranks);
    }

    public static BottomRanks generate(int n) {
        return generate(n, new Random());
    }

    public static BottomRanks generate(int n, Random random) {
        if (n <= 0) {
            throw new BusinessException(LadderGameErrorCode.INVALID_PLAYER_COUNT, "플레이어 수는 1 이상이어야 합니다");
        }
        final List<Integer> rankList = new ArrayList<>(IntStream.rangeClosed(1, n).boxed().toList());
        Collections.shuffle(rankList, random);
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
