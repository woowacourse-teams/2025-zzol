package coffeeshout.racinggame.domain.event;

import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.RacingGameState;

public record RaceStateChangedEvent(String joinCode, RacingGameState state) {

    public static RaceStateChangedEvent of(RacingGame racingGame, String joinCode) {
        return new RaceStateChangedEvent(joinCode, racingGame.getState());
    }
}
