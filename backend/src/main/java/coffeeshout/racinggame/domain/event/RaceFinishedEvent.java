package coffeeshout.racinggame.domain.event;

import coffeeshout.racinggame.domain.RacingGame;

public record RaceFinishedEvent(String state, String joinCode) {

    public static RaceFinishedEvent of(RacingGame racingGame, String joinCode) {
        return new RaceFinishedEvent(racingGame.getState().name(), joinCode);
    }
}
