package coffeeshout.cardgame.domain;

import coffeeshout.cardgame.domain.card.Card;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CardHand {

    private final List<Card> hand;

    public CardHand() {
        this.hand = new ArrayList<>();
    }

    public CardGameScore calculateCardGameScore() {
        return new CardGameScore(this);
    }

    public void put(Card card) {
        hand.add(card);
    }

    public int size() {
        return hand.size();
    }

    public void forEach(Consumer<Card> consumer) {
        hand.forEach(consumer);
    }

    public Card getCard(int index) {
        return hand.get(index);
    }

    public boolean isSelected(CardGameRound round) {
        return round.toIndex() <= hand.size();
    }

    public boolean isAssign(Card card, CardGameRound round) {
        if (round.isReady() || round.toIndex() > hand.size()) {
            return false;
        }
        return hand.get(round.toIndex() - 1).equals(card);
    }
}
