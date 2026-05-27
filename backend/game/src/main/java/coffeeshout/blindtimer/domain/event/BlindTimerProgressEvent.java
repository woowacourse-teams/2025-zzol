package coffeeshout.blindtimer.domain.event;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import java.util.List;

public record BlindTimerProgressEvent(String joinCode, List<BlindTimerPlayerProgress> players) {

    public record BlindTimerPlayerProgress(String playerName, boolean stopped, boolean timedOut) {
    }

    public static BlindTimerProgressEvent of(BlindTimerGame game, String joinCode) {
        final List<BlindTimerPlayerProgress> progresses = game.getPlayers().stream()
                .map(p -> new BlindTimerPlayerProgress(
                        p.getPlayer().getName().value(),
                        p.isStopped(),
                        p.isTimedOut()
                ))
                .toList();
        return new BlindTimerProgressEvent(joinCode, progresses);
    }
}
