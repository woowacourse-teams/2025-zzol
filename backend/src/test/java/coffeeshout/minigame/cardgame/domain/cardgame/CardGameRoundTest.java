package coffeeshout.minigame.cardgame.domain.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.cardgame.domain.CardGameRound;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CardGameRoundTest {

    @Test
    void 다음_라운드로_진행한다() {
        // given & when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(CardGameRound.READY.next()).isEqualTo(CardGameRound.FIRST);
            softly.assertThat(CardGameRound.FIRST.next()).isEqualTo(CardGameRound.SECOND);
            softly.assertThat(CardGameRound.SECOND.next()).isEqualTo(CardGameRound.END);
        });
    }

    @Test
    void 마지막_라운드에서_다음_라운드로_진행하면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> CardGameRound.END.next())
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "READY, 0",
            "FIRST, 1",
            "SECOND, 2",
            "END, 3"
    })
    void 라운드를_정수로_변환한다(CardGameRound round, int expected) {
        // when
        int result = round.toInteger();

        // then
        assertThat(result).isEqualTo(expected);
    }
}
