package coffeeshout.room.domain.player;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ColorUsage {

    private static final int COLOR_MAX_COUNT = 9;

    private final Random random;
    private final Map<Integer, Boolean> colors;

    public ColorUsage(String joinCode) {
        this.random = new Random(joinCode.hashCode());
        this.colors = new ConcurrentHashMap<>();
        for (int i = 0; i < COLOR_MAX_COUNT; i++) {
            colors.put(i, false);
        }
    }

    public int pickRandomOne() {
        List<Integer> available = colors.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .toList();

        if (available.isEmpty()) {
            throw new InvalidArgumentException(ColorErrorCode.NO_AVAILABLE_COLOR, "사용가능한 색깔을 찾지 못했습니다.");
        }

        final Integer colorIndex = available.get(random.nextInt(available.size()));
        colors.put(colorIndex, true);
        return colorIndex;
    }

    public void release(int colorIndex) {
        if (colorIndex < 0 || colorIndex >= COLOR_MAX_COUNT) {
            throw new InvalidArgumentException(ColorErrorCode.INVALID_COLOR_INDEX, "유효하지 않은 색깔 index입니다.");
        }
        colors.put(colorIndex, false);
    }
}
