package coffeeshout.fixture;

import coffeeshout.room.domain.roulette.RandomPicker;

public class FixedLastValueGenerator implements RandomPicker {

    @Override
    public int nextInt(int origin, int bound) {
        return bound;
    }
}
