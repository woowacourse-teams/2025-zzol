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
        return round.toInteger() <= hand.size();
    }

    public boolean isAssign(Card card, CardGameRound round) {
        if (round == CardGameRound.READY || round.toInteger() > hand.size()) {
            return false;
        }
        return hand.get(round.toInteger() - 1).equals(card);
    }
}

