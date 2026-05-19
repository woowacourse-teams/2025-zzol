package coffeeshout.room.domain.roulette;

import static org.springframework.util.Assert.state;

import coffeeshout.minigame.domain.MiniGameResultType;

/*
    - 확률은 100.00 형태에 100을 곱해서 소수점을 없앤 형태로 사용한다.
    - 따라서 value는 0 ~ 10000값을 가진다.
 */
public record Probability(Integer value) {

    public static final Probability ZERO = new Probability(0);
    public static final Probability TOTAL = new Probability(10000);
    private static final int MIN_PROBABILITY = 0;
    private static final int MAX_PROBABILITY = 10000;

    public Probability {
        state(value >= MIN_PROBABILITY && value <= MAX_PROBABILITY, "확률은 0 ~ 10000 사이어야 합니다.");
    }

    public Probability divide(int number) {
        return new Probability(value / number);
    }

    public Probability multiple(int number) {
        return new Probability(value * number);
    }

    public Probability multiple(double number) {
        return new Probability((int) (value * number));
    }

    public Probability minus(Probability other) {
        return new Probability(this.value - other.value);
    }

    public Probability plus(Probability other) {
        return new Probability(this.value + other.value);
    }

    public Probability plus(int value) {
        return new Probability(this.value + value);
    }

    public int getProbabilityChange(MiniGameResultType resultType) {
        if (resultType == MiniGameResultType.WINNER) {
            return -value;
        }
        return value;
    }
}
