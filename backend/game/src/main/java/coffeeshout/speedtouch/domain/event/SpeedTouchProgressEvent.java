package coffeeshout.speedtouch.domain.event;

import coffeeshout.speedtouch.domain.SpeedTouchGame;
import coffeeshout.speedtouch.domain.SpeedTouchPlayer;
import java.util.List;

public record SpeedTouchProgressEvent(String joinCode, List<SpeedTouchPlayerProgress> players) {

    public record SpeedTouchPlayerProgress(String playerName, int currentNumber, boolean finished) {
    }

    public static SpeedTouchProgressEvent of(SpeedTouchGame game, String joinCode) {
        final List<SpeedTouchPlayerProgress> progresses = game.getPlayers().stream()
                .map(p -> new SpeedTouchPlayerProgress(
                        p.getPlayer().getName().value(),
                        p.getCurrentNumber(),
                        p.isFinished()
                ))
                .toList();
        return new SpeedTouchProgressEvent(joinCode, progresses);
    }
}
