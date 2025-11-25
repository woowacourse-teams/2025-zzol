package coffeeshout.minigame.cardgame.domain.cardgame;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.cardgame.domain.CardHand;
import coffeeshout.cardgame.domain.card.AdditionCard;
import coffeeshout.cardgame.domain.card.MultiplierCard;
import org.junit.jupiter.api.Test;

class CardGameScoreTest {

    @Test
    void 카드_핸드로_점수를_생성한다() {
        // given
        CardHand hand = new CardHand();
        hand.put(AdditionCard.PLUS_30);
        hand.put(MultiplierCard.DOUBLE);

        // when
        CardGameScore score = new CardGameScore(hand);

        // then
        assertThat(score.getValue()).isEqualTo(60);
    }

    @Test
    void 덧셈_카드로_점수를_업데이트한다() {
        // given
        CardHand hand = new CardHand();
        hand.put(AdditionCard.PLUS_40);

        // when
        CardGameScore score = hand.calculateCardGameScore();

        // then
        assertThat(score.getValue()).isEqualTo(40);
    }

    @Test
    void 곱셈_카드로_점수를_업데이트한다() {
        // given
        CardHand hand = new CardHand();
        hand.put(AdditionCard.PLUS_10);
        hand.put(MultiplierCard.DOUBLE);

        // when
        CardGameScore score = hand.calculateCardGameScore();

        // then
        assertThat(score.getValue()).isEqualTo(20);
    }

    @Test
    void 음수_덧셈_카드로_점수를_업데이트한다() {
        // given
        CardHand hand = new CardHand();
        hand.put(AdditionCard.PLUS_30);
        hand.put(AdditionCard.MINUS_10);

        // when
        CardGameScore score = hand.calculateCardGameScore();

        // then
        assertThat(score.getValue()).isEqualTo(20);
    }
}
