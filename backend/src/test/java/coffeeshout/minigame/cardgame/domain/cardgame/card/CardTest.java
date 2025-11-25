package coffeeshout.minigame.cardgame.domain.cardgame.card;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.cardgame.domain.card.AdditionCard;
import coffeeshout.cardgame.domain.card.Card;
import coffeeshout.cardgame.domain.card.MultiplierCard;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CardTest {

    @ParameterizedTest
    @MethodSource
    void 동일한_Card는_종류와_값이_같아야_한다(Card card, Card otherCard) {
        // given

        // when & then
        assertThat(card).isEqualTo(otherCard);
    }

    static Stream<Arguments> 동일한_Card는_종류와_값이_같아야_한다() {
        return Stream.of(
                Arguments.of(new AdditionCard(10), new AdditionCard(10)),
                Arguments.of(new AdditionCard(-40), new AdditionCard(-40)),
                Arguments.of(new MultiplierCard(4), new MultiplierCard(4)),
                Arguments.of(new MultiplierCard(0), new MultiplierCard(0)),
                Arguments.of(new MultiplierCard(-1), new MultiplierCard(-1))
        );
    }
}
