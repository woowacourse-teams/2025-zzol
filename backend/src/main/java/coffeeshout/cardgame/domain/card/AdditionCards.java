package coffeeshout.cardgame.domain.card;

import static coffeeshout.cardgame.domain.card.AdditionCard.MINUS_10;
import static coffeeshout.cardgame.domain.card.AdditionCard.MINUS_15;
import static coffeeshout.cardgame.domain.card.AdditionCard.MINUS_20;
import static coffeeshout.cardgame.domain.card.AdditionCard.MINUS_25;
import static coffeeshout.cardgame.domain.card.AdditionCard.MINUS_30;
import static coffeeshout.cardgame.domain.card.AdditionCard.MINUS_35;
import static coffeeshout.cardgame.domain.card.AdditionCard.MINUS_40;
import static coffeeshout.cardgame.domain.card.AdditionCard.MINUS_5;
import static coffeeshout.cardgame.domain.card.AdditionCard.PLUS_10;
import static coffeeshout.cardgame.domain.card.AdditionCard.PLUS_15;
import static coffeeshout.cardgame.domain.card.AdditionCard.PLUS_20;
import static coffeeshout.cardgame.domain.card.AdditionCard.PLUS_25;
import static coffeeshout.cardgame.domain.card.AdditionCard.PLUS_30;
import static coffeeshout.cardgame.domain.card.AdditionCard.PLUS_35;
import static coffeeshout.cardgame.domain.card.AdditionCard.PLUS_40;
import static coffeeshout.cardgame.domain.card.AdditionCard.PLUS_5;
import static coffeeshout.cardgame.domain.card.AdditionCard.ZERO;
import static org.springframework.util.Assert.isTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AdditionCards {

    private final List<Card> cards;

    public AdditionCards() {
        this.cards = new ArrayList<>();
        this.cards.add(PLUS_40);
        this.cards.add(PLUS_35);
        this.cards.add(PLUS_30);
        this.cards.add(PLUS_25);
        this.cards.add(PLUS_20);
        this.cards.add(PLUS_15);
        this.cards.add(PLUS_10);
        this.cards.add(PLUS_5);
        this.cards.add(ZERO);
        this.cards.add(MINUS_5);
        this.cards.add(MINUS_10);
        this.cards.add(MINUS_15);
        this.cards.add(MINUS_20);
        this.cards.add(MINUS_25);
        this.cards.add(MINUS_30);
        this.cards.add(MINUS_35);
        this.cards.add(MINUS_40);
    }

    public List<Card> pickCards(int count, Random random) {
        isTrue(count <= cards.size(), "최대 사용 가능한 카드 수를 초과했습니다. size = " + cards.size());
        List<Card> shuffledCards = new ArrayList<>(cards);
        Collections.shuffle(shuffledCards, random);
        return shuffledCards.subList(0, count);
    }
}
