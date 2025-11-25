package coffeeshout.cardgame.domain;

import static org.springframework.util.Assert.state;

import lombok.Getter;

@Getter
public enum CardGameRound {
    READY,
    FIRST,
    SECOND,
    END,
    ;

    public CardGameRound next() {
        state(this.ordinal() != values().length - 1, "마지막 라운드입니다.");
        final int currentRound = this.ordinal();
        CardGameRound[] values = values();
        return values[currentRound + 1];
    }

    public int toInteger() {
        return ordinal();
    }
}
