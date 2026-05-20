package coffeeshout.blindtimer.domain;

import coffeeshout.room.domain.player.PlayerName;
import java.time.Duration;
import lombok.Getter;

@Getter
public class BlindTimerPlayer {

    private final PlayerName playerName;
    private Duration stoppedElapsed;
    private boolean timedOut;

    public BlindTimerPlayer(PlayerName playerName) {
        this.playerName = playerName;
    }

    public synchronized boolean stop(Duration elapsed) {
        if (isStopped()) {
            return false;
        }
        this.stoppedElapsed = elapsed;
        return true;
    }

    public synchronized void markTimedOut() {
        if (!isStopped()) {
            this.timedOut = true;
        }
    }

    public synchronized boolean isStopped() {
        return stoppedElapsed != null || timedOut;
    }

    public synchronized boolean isTimedOut() {
        return timedOut;
    }

    public synchronized Duration getStoppedElapsed() {
        return stoppedElapsed;
    }
}
