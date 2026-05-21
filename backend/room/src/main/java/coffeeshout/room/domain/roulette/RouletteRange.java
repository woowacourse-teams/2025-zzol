package coffeeshout.room.domain.roulette;

import coffeeshout.room.domain.player.Player;

public record RouletteRange(int start, int end, Player player) {

    public boolean isBetween(int number) {
        return number >= start && number <= end;
    }
}
