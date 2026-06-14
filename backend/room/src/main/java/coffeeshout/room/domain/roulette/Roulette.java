package coffeeshout.room.domain.roulette;

import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.Players;
import coffeeshout.room.domain.player.Winner;

public class Roulette {

    private final RandomPicker randomPicker;

    public Roulette(RandomPicker randomGenerator) {
        this.randomPicker = randomGenerator;
    }

    public Winner spin(Players players) {
        final RouletteRanges rouletteRanges = new RouletteRanges(players);
        final int randomNumber = randomPicker.nextInt(1, rouletteRanges.endValue());
        final Player pickedPlayer = rouletteRanges.pickPlayer(randomNumber);
        return Winner.from(pickedPlayer);
    }
}
