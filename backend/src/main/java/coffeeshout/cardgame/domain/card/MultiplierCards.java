package coffeeshout.cardgame.domain.card;

import static coffeeshout.cardgame.domain.card.MultiplierCard.DOUBLE;
import static coffeeshout.cardgame.domain.card.MultiplierCard.INVERT;
import static coffeeshout.cardgame.domain.card.MultiplierCard.QUADRUPLE;
import static org.springframework.util.Assert.isTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MultiplierCards {

    private final List<Card> cards;

    public MultiplierCards() {
        this.cards = new ArrayList<>();
        this.cards.add(QUADRUPLE);
        this.cards.add(DOUBLE);
        this.cards.add(INVERT);
    }

    public List<Card> pickCards(int count, Random random) {
        isTrue(count <= cards.size(), "최대 사용 가능한 카드 수를 초과했습니다. size = " + cards.size());
        List<Card> shuffledCards = new ArrayList<>(cards);
        Collections.shuffle(shuffledCards, random);
        return shuffledCards.subList(0, count);
    }
}
