package coffeeshout.fixture;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.card.CardGameDeckGenerator;

public class CardGameFake extends CardGame {

    public CardGameFake(CardGameDeckGenerator deckGenerator) {
        super(deckGenerator, 1234L); // 테스트용 고정 시드
    }
}
