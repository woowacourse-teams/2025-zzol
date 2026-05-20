package coffeeshout.room.domain.roulette;

import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.Players;
import java.util.ArrayList;
import java.util.List;

public class RouletteRanges {

    private final List<RouletteRange> ranges;

    public RouletteRanges(Players players) {
        this.ranges = new ArrayList<>();

        players.getPlayers().forEach(player -> ranges.add(generateRange(
                endValue() + 1,
                player.getProbability().value(),
                player
        )));
    }

    public Player pickPlayer(int number) {
        return ranges.stream()
                .filter(rouletteRange -> rouletteRange.isBetween(number))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("범위에 해당하지 않는 숫자입니다."))
                .player();
    }

    public int endValue() {
        if (ranges.isEmpty()) {
            return 0;
        }
        return ranges.getLast().end();
    }

    private RouletteRange generateRange(int start, int gap, Player player) {
        return new RouletteRange(start, start + gap - 1, player);
    }
}
