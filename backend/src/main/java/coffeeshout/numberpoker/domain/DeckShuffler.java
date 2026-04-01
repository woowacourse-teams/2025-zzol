package coffeeshout.numberpoker.domain;

import java.util.List;

/**
 * 포커 덱 셔플 전략.
 * 도메인이 java.util.Random에 직접 의존하지 않도록 분리한다.
 */
@FunctionalInterface
public interface DeckShuffler {
    void shuffle(List<PokerCard> deck);
}
