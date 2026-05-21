package coffeeshout.fixture;

import coffeeshout.room.domain.roulette.Roulette;
import coffeeshout.room.domain.roulette.RoulettePicker;

public final class RouletteFixture {

    private RouletteFixture() {
    }

    public static Roulette 고정_끝값_반환() {
        final Roulette roulette = new Roulette(new FixedLastValueGenerator());
        return roulette;
    }

    public static Roulette 랜덤_반환() {
        final Roulette roulette = new Roulette(new RoulettePicker());
        return roulette;
    }
}
