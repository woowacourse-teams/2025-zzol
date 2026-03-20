package coffeeshout.racinggame.domain.event;

import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.RacingGameState;

public record RaceFinishedEvent(RacingGameState state, String joinCode) {

    public static RaceFinishedEvent of(RacingGame racingGame, String joinCode) {
        return new RaceFinishedEvent(racingGame.getState(), joinCode);
    }
}
