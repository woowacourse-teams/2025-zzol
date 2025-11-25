package coffeeshout.minigame.cardgame.domain.cardgame.card;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import coffeeshout.cardgame.domain.card.AdditionCard;
import coffeeshout.cardgame.domain.card.AdditionCards;
import coffeeshout.cardgame.domain.card.Card;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AdditionCardsTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void 요청한_개수만큼_카드를_뽑는다(int count) {
        // given
        AdditionCards additionCards = new AdditionCards();

        // when
        List<Card> pickedCards = additionCards.pickCards(count, new java.util.Random(1234L));

        // then
        assertThat(pickedCards).hasSize(count);
        assertThat(pickedCards).allMatch(card -> card instanceof AdditionCard);
    }

    @Test
    void 최대_사용_가능한_카드_수를_초과하면_예외가_발생한다() {
        // given
        AdditionCards additionCards = new AdditionCards();
        int count = 20;

        // when & then
        assertThatThrownBy(() -> additionCards.pickCards(count, new java.util.Random(1234L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 사용 가능한 카드 수를 초과했습니다.");

    }
}
