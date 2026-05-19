package coffeeshout.cardgame.domain.card;

import static org.springframework.util.Assert.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class Deck {

    private final List<Card> cards;
    private final List<Card> pickedCards;

    public Deck(@NonNull List<Card> additionCards, @NonNull List<Card> multiplierCards) {
        this.cards = new ArrayList<>();
        this.pickedCards = new ArrayList<>();
        this.cards.addAll(additionCards);
        this.cards.addAll(multiplierCards);
    }

    public void shuffle(Random random) {
        Collections.shuffle(cards, random);
        pickedCards.clear();
    }

    public void shuffle() {
        shuffle(new Random());
    }

    public Card pick(int cardIndex) {
        final Card selectedCard = cards.get(cardIndex);
        state(!isPicked(selectedCard), "이미 뽑은 카드입니다.");
        pickedCards.add(selectedCard);
        return selectedCard;
    }

    public Card pickRandom() {
        final List<Card> remainingCards = getRemainingCards();
        final Card selected = pickRandom(remainingCards);
        return pick(cards.indexOf(selected));
    }

    public Card pickRandom(java.util.Random random) {
        final List<Card> remainingCards = getRemainingCards();
        final Card selected = pickRandom(remainingCards, random);
        return pick(cards.indexOf(selected));
    }

    private boolean isPicked(Card card) {
        return pickedCards.contains(card);
    }

    public Stream<Card> stream() {
        return cards.stream();
    }

    public int size() {
        return cards.size();
    }

    private List<Card> getRemainingCards() {
        final List<Card> cloned = new ArrayList<>(cards);
        cloned.removeAll(pickedCards);
        return cloned;
    }

    private Card pickRandom(List<Card> cards) {
        final int randomNumber = ThreadLocalRandom.current().nextInt(0, cards.size());
        return cards.get(randomNumber);
    }

    private Card pickRandom(List<Card> cards, java.util.Random random) {
        final int randomNumber = random.nextInt(0, cards.size());
        return cards.get(randomNumber);
    }
}
