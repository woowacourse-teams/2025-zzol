package coffeeshout.numberpoker.domain;

import coffeeshout.global.exception.custom.BusinessException;
import java.util.List;

public class Dealer {

    private final PokerCard firstCard;
    private final PokerCard secondCard;
    private int revealedCount;

    public Dealer(PokerCard firstCard, PokerCard secondCard) {
        this.firstCard = firstCard;
        this.secondCard = secondCard;
        this.revealedCount = 0;
    }

    public void revealFirst() {
        if (revealedCount >= 1) {
            throw new BusinessException(
                    NumberPokerErrorCode.INVALID_PHASE_ACTION,
                    "딜러 카드가 이미 공개되었습니다. revealedCount=" + revealedCount
            );
        }
        this.revealedCount = 1;
    }

    public void revealAll() {
        this.revealedCount = 2;
    }

    public List<PokerCard> getVisibleCards() {
        return switch (revealedCount) {
            case 0 -> List.of();
            case 1 -> List.of(firstCard);
            default -> List.of(firstCard, secondCard);
        };
    }

    public HandRanking getHandRanking() {
        return HandRanking.of(firstCard, secondCard);
    }

    public int getHiddenCount() {
        return 2 - revealedCount;
    }

    public boolean isFullyRevealed() {
        return revealedCount == 2;
    }
}
