package coffeeshout.numberpoker.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.player.Player;

public class PlayerPokerHand {

    private final Player player;
    private final PokerCard firstCard;
    private final PokerCard secondCard;
    private boolean folded;
    private PokerRoundResult foldResult;

    public PlayerPokerHand(Player player, PokerCard firstCard, PokerCard secondCard) {
        this.player = player;
        this.firstCard = firstCard;
        this.secondCard = secondCard;
        this.folded = false;
        this.foldResult = null;
    }

    public void fold(PokerPhase currentPhase) {
        if (folded) {
            throw new BusinessException(
                    NumberPokerErrorCode.ALREADY_FOLDED,
                    "이미 폴드한 플레이어입니다. player=" + player.getName()
            );
        }
        this.folded = true;
        this.foldResult = currentPhase == PokerPhase.STAGE_1
                ? PokerRoundResult.STAGE_1_FOLD
                : PokerRoundResult.STAGE_2_FOLD;
    }

    public PokerRoundResult determineResult(HandRanking dealerRanking) {
        if (folded) {
            return foldResult;
        }
        final int cmp = getHandRanking().compareTo(dealerRanking);
        if (cmp > 0) {
            return PokerRoundResult.WIN;
        }
        if (cmp < 0) {
            return PokerRoundResult.LOSE;
        }
        return PokerRoundResult.TIE;
    }

    public HandRanking getHandRanking() {
        return HandRanking.of(firstCard, secondCard);
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isActive() {
        return !folded;
    }

    public boolean isFolded() {
        return folded;
    }

    public PokerRoundResult getFoldResult() {
        return foldResult;
    }
}
