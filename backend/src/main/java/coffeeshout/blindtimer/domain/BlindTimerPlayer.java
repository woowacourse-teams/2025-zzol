package coffeeshout.blindtimer.domain;

import coffeeshout.room.domain.player.Player;
import lombok.Getter;

@Getter
public class BlindTimerPlayer {

    private final Player player;
    private Long stoppedElapsedMillis;
    private boolean timedOut;

    public BlindTimerPlayer(Player player) {
        this.player = player;
    }

    public synchronized boolean stop(long elapsedMillis) {
        if (isStopped()) {
            return false;
        }
        this.stoppedElapsedMillis = elapsedMillis;
        return true;
    }

    public synchronized void markTimedOut() {
        if (!isStopped()) {
            this.timedOut = true;
        }
    }

    public synchronized boolean isStopped() {
        return stoppedElapsedMillis != null || timedOut;
    }

    public synchronized boolean isTimedOut() {
        return timedOut;
    }

    public synchronized Long getStoppedElapsedMillis() {
        return stoppedElapsedMillis;
    }
}
