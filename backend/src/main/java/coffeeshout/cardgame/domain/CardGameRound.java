package coffeeshout.cardgame.domain;

import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.util.Assert;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardGameRound {

    private final int number; // 0 = READY, 1 = 첫 번째 라운드, ...
    private final int total;  // 게임 라운드 수

    public static CardGameRound ready(int totalRounds) {
        return new CardGameRound(0, totalRounds);
    }

    public static CardGameRound roundOf(int number, int totalRounds) {
        return new CardGameRound(number, totalRounds);
    }

    public boolean isReady() {
        return number == 0;
    }

    public boolean isFirst() {
        return number == 1;
    }

    public boolean isLast() {
        return number == total;
    }

    public CardGameRound next() {
        Assert.state(number < total, "마지막 라운드입니다.");
        return new CardGameRound(number + 1, total);
    }

    public int toIndex() {
        return number;
    }

    public int getTotalRounds() {
        return total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CardGameRound that)) {
            return false;
        }
        return number == that.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    @Override
    public String toString() {
        return "CardGameRound{number=" + number + ", total=" + total + "}";
    }
}
