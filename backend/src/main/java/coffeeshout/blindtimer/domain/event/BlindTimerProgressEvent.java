package coffeeshout.blindtimer.domain.event;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import java.util.List;

public record BlindTimerProgressEvent(String joinCode, List<PlayerProgress> players) {

    public record PlayerProgress(String playerName, boolean stopped, boolean timedOut) {
    }

    public static BlindTimerProgressEvent of(BlindTimerGame game, String joinCode) {
        final List<PlayerProgress> progresses = game.getPlayers().stream()
                .map(p -> new PlayerProgress(
                        p.getPlayer().getName().value(),
                        p.isStopped(),
                        p.isTimedOut()
                ))
                .toList();
        return new BlindTimerProgressEvent(joinCode, progresses);
    }
}
