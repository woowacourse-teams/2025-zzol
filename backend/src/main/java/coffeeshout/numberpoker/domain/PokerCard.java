package coffeeshout.numberpoker.domain;

import coffeeshout.global.exception.custom.BusinessException;

public record PokerCard(int value) {

    private static final int MIN_VALUE = 1;
    private static final int MAX_VALUE = 10;

    public PokerCard {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new BusinessException(
                    NumberPokerErrorCode.INVALID_CARD_VALUE,
                    "카드 값은 1~10이어야 합니다. value=" + value
            );
        }
    }
}
