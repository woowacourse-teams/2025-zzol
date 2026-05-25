package coffeeshout.fixture;

import coffeeshout.room.domain.roulette.RandomPicker;

public class StubRandomPicker implements RandomPicker {

    @Override
    public int nextInt(int origin, int bound) {
        return bound;
    }
}
