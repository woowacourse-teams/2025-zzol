package coffeeshout.speedtouch.domain;

import coffeeshout.minigame.domain.Gamer;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;

public class SpeedTouchPlayer {

    public static final int FIRST_NUMBER = 1;
    public static final int LAST_NUMBER = 25;

    @Getter
    private final Gamer gamer;
    private int currentNumber;
    private Instant finishTime;

    public SpeedTouchPlayer(Gamer gamer) {
        this.gamer = gamer;
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

    public synchronized boolean isFinished() {
        return finishTime != null;
    }

    public synchronized int getCurrentNumber() {
        return currentNumber;
    }

    public synchronized int getProgress() {
        return currentNumber - FIRST_NUMBER;
    }

    public synchronized Instant getFinishTime() {
        return finishTime;
    }

    public synchronized long calculateFinishMillis(Instant startTime) {
        if (!isFinished()) {
            throw new IllegalStateException("완주하지 않은 플레이어의 완주 시간을 계산할 수 없습니다.");
        }
        return Duration.between(startTime, finishTime).toMillis();
    }
}
