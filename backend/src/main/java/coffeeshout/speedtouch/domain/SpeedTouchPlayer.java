package coffeeshout.speedtouch.domain;

import coffeeshout.room.domain.player.Player;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;

@Getter
public class SpeedTouchPlayer {

    public static final int FIRST_NUMBER = 1;
    public static final int LAST_NUMBER = 25;

    private final Player player;
    private int currentNumber;
    private Instant finishTime;

    public SpeedTouchPlayer(Player player) {
        this.player = player;
        this.currentNumber = FIRST_NUMBER;
    }

    public synchronized boolean touch(int number, Instant now) {
        if (isFinished()) {
            return false;
        }
        if (number != currentNumber) {
            return false;
        }
        currentNumber++;
        if (currentNumber > LAST_NUMBER) {
            finishTime = now;
        }
        return true;
    }

    public boolean isFinished() {
        return finishTime != null;
    }

    public int getProgress() {
        return currentNumber - FIRST_NUMBER;
    }

    public long calculateFinishMillis(Instant startTime) {
        if (!isFinished()) {
            throw new IllegalStateException("완주하지 않은 플레이어의 완주 시간을 계산할 수 없습니다.");
        }
        return Duration.between(startTime, finishTime).toMillis();
    }
}
