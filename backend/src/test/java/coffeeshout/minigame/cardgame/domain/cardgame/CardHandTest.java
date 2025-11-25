package coffeeshout.minigame.cardgame.domain.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.cardgame.domain.CardGameRound;
import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.cardgame.domain.CardHand;
import coffeeshout.cardgame.domain.card.AdditionCard;
import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.cardgame.domain.card.MultiplierCard;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CardHandTest {

    private CardHand cardHand;

    @BeforeEach
    void setUp() {
        cardHand = new CardHand();
    }

    @Test
    void 가지고_있는_카드들의_점수합을_반환한다() {
        // given
        cardHand.put(new AdditionCard(10));
        cardHand.put(new MultiplierCard(-2));
        cardHand.put(new AdditionCard(30));

        // when
        CardGameScore cardGameScore = cardHand.calculateCardGameScore();

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cardGameScore.getValue()).isEqualTo(-80);
            softly.assertThat(cardHand.size()).isEqualTo(3);
        });
    }

    @Test
    void 해당_라운드에_해당_카드를_뽑았으면_true를_반환한다() {
        // given
        Card firstRoundCard = new AdditionCard(10);
        Card secondRoundCard = new MultiplierCard(-2);
        Card notPickedCard = new MultiplierCard(1);
        cardHand.put(firstRoundCard);
        cardHand.put(secondRoundCard);

        // when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cardHand.isAssign(firstRoundCard, CardGameRound.FIRST)).isTrue();
            softly.assertThat(cardHand.isAssign(secondRoundCard, CardGameRound.SECOND)).isTrue();
            softly.assertThat(cardHand.isAssign(notPickedCard, CardGameRound.FIRST)).isFalse();
            softly.assertThat(cardHand.isAssign(notPickedCard, CardGameRound.SECOND)).isFalse();
        });
    }

    @Test
    void 해당_라운드에_카드를_뽑았으면_true를_반환한다() {
        // given
        cardHand.put(new AdditionCard(10));

        // when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cardHand.isSelected(CardGameRound.FIRST)).isTrue();
            softly.assertThat(cardHand.isSelected(CardGameRound.SECOND)).isFalse();
        });
    }

    @Nested
    class 카드_추가_테스트 {

        @Test
        void 카드를_추가한다() {
            // given
            Card card = AdditionCard.PLUS_40;

            // when
            cardHand.put(card);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardHand.size()).isEqualTo(1);
                softly.assertThat(cardHand.getCard(0)).isEqualTo(card);
            });
        }

        @Test
        void 여러_카드를_추가한다() {
            // given
            Card card1 = AdditionCard.PLUS_40;
            Card card2 = MultiplierCard.DOUBLE;

            // when
            cardHand.put(card1);
            cardHand.put(card2);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardHand.size()).isEqualTo(2);
                softly.assertThat(cardHand.getCard(0)).isEqualTo(card1);
                softly.assertThat(cardHand.getCard(1)).isEqualTo(card2);
            });
        }
    }

    @Nested
    class 카드_조회_테스트 {

        @Test
        void 인덱스로_카드를_조회한다() {
            // given
            Card card = AdditionCard.PLUS_30;
            cardHand.put(card);

            // when
            Card retrievedCard = cardHand.getCard(0);

            // then
            assertThat(retrievedCard).isEqualTo(card);
        }

        @Test
        void 잘못된_인덱스로_카드를_조회하면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> cardHand.getCard(0))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Nested
    class 점수_계산_테스트 {

        @Test
        void 빈_핸드의_점수를_계산한다() {
            // when
            CardGameScore score = cardHand.calculateCardGameScore();

            // then
            assertThat(score.getValue()).isEqualTo(0);
        }

        @Test
        void 덧셈_카드만_있는_핸드의_점수를_계산한다() {
            // given
            cardHand.put(AdditionCard.PLUS_40);
            cardHand.put(AdditionCard.PLUS_30);

            // when
            CardGameScore score = cardHand.calculateCardGameScore();

            // then
            assertThat(score.getValue()).isEqualTo(70);
        }
    }

    @Nested
    class 라운드_선택_확인_테스트 {

        @Test
        void 첫번째_라운드에서_선택했는지_확인한다() {
            // given
            cardHand.put(AdditionCard.PLUS_40);

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardHand.isSelected(CardGameRound.FIRST)).isTrue();
                softly.assertThat(cardHand.isSelected(CardGameRound.SECOND)).isFalse();
            });
        }

        @Test
        void 두번째_라운드에서_선택했는지_확인한다() {
            // given
            cardHand.put(AdditionCard.PLUS_40); // 첫 번째 라운드
            cardHand.put(MultiplierCard.DOUBLE); // 두 번째 라운드

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardHand.isSelected(CardGameRound.FIRST)).isTrue();
                softly.assertThat(cardHand.isSelected(CardGameRound.SECOND)).isTrue();
            });
        }

        @Test
        void 아무_카드도_선택하지_않았을_때_확인한다() {
            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardHand.isSelected(CardGameRound.FIRST)).isFalse();
                softly.assertThat(cardHand.isSelected(CardGameRound.SECOND)).isFalse();
            });
        }
    }

    @Nested
    class 카드_할당_확인_테스트 {

        @Test
        void 첫번째_라운드에_할당된_카드인지_확인한다() {
            // given
            Card card1 = AdditionCard.PLUS_40;
            Card card2 = MultiplierCard.DOUBLE;
            cardHand.put(card1);
            cardHand.put(card2);

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardHand.isAssign(card1, CardGameRound.FIRST)).isTrue();
                softly.assertThat(cardHand.isAssign(card2, CardGameRound.FIRST)).isFalse();
            });
        }

        @Test
        void 두번째_라운드에_할당된_카드인지_확인한다() {
            // given
            Card card1 = AdditionCard.PLUS_40;
            Card card2 = MultiplierCard.DOUBLE;
            cardHand.put(card1);
            cardHand.put(card2);

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardHand.isAssign(card1, CardGameRound.SECOND)).isFalse();
                softly.assertThat(cardHand.isAssign(card2, CardGameRound.SECOND)).isTrue();
            });
        }

        @Test
        void 존재하지_않는_카드는_할당되지_않은_카드이다() {
            // given
            Card existingCard = AdditionCard.PLUS_40;
            Card nonExistentCard = AdditionCard.PLUS_30;
            cardHand.put(existingCard);

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardHand.isAssign(existingCard, CardGameRound.FIRST)).isTrue();
                softly.assertThat(cardHand.isAssign(nonExistentCard, CardGameRound.FIRST)).isFalse();
            });
        }

        @Test
        void 라운드_범위를_벗어나면_할당되지_않은_카드이다() {
            // given
            Card card = AdditionCard.PLUS_40;
            cardHand.put(card);

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(cardHand.isAssign(card, CardGameRound.FIRST)).isTrue();
                softly.assertThat(cardHand.isAssign(card, CardGameRound.SECOND)).isFalse();
            });
        }
    }
}
