package coffeeshout.racinggame.domain.event;

import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.RacingRange;
import coffeeshout.racinggame.domain.RunnerPosition;
import java.util.List;

public record RunnersMovedEvent(String joinCode, RacingRange racingRange, List<RunnerPosition> runnerPositions) {

    public static RunnersMovedEvent of(RacingGame racingGame, String joinCode) {
        final RacingRange distance = new RacingRange(RacingGame.START_LINE, RacingGame.FINISH_LINE);
        final List<RunnerPosition> positions = racingGame.getRunners().stream()
                .map(runner -> new RunnerPosition(
                        runner.getPlayer().getName().value(),
                        runner.getPosition(),
                        runner.getSpeed()
                )).toList();
        return new RunnersMovedEvent(joinCode, distance, positions);
    }
}
