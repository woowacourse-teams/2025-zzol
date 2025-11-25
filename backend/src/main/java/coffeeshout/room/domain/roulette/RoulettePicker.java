package coffeeshout.room.domain.roulette;

import java.util.Random;

public class RoulettePicker implements RandomPicker {

    private final Random random = new Random();

    @Override
    public int nextInt(int origin, int bound) {
        return random.nextInt(origin, bound + 1);
    }
}
