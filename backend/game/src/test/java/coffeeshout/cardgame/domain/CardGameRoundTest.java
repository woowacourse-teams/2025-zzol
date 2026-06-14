package coffeeshout.cardgame.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class CardGameRoundTest {

    @Test
    void READY_상태에서_첫_번째_라운드로_진행한다() {
        CardGameRound round = CardGameRound.ready(2).next();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(round.isFirst()).isTrue();
            softly.assertThat(round.isLast()).isFalse();
            softly.assertThat(round.toIndex()).isEqualTo(1);
        });
    }

    @Test
    void 라운드를_순서대로_끝까지_진행한다() {
        CardGameRound first = CardGameRound.ready(2).next();
        CardGameRound second = first.next();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(first.isFirst()).isTrue();
            softly.assertThat(first.isLast()).isFalse();
            softly.assertThat(second.isFirst()).isFalse();
            softly.assertThat(second.isLast()).isTrue();
        });
    }

    @Test
    void 마지막_라운드에서_다음으로_진행하면_예외가_발생한다() {
        CardGameRound lastRound = CardGameRound.roundOf(2, 2);

        assertThatThrownBy(lastRound::next)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void toIndex는_라운드_번호를_반환한다() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(CardGameRound.ready(2).toIndex()).isEqualTo(0);
            softly.assertThat(CardGameRound.roundOf(1, 2).toIndex()).isEqualTo(1);
            softly.assertThat(CardGameRound.roundOf(2, 2).toIndex()).isEqualTo(2);
        });
    }

    @Test
    void READY_상태를_확인한다() {
        assertThat(CardGameRound.ready(2).isReady()).isTrue();
        assertThat(CardGameRound.roundOf(1, 2).isReady()).isFalse();
    }

    @Test
    void 총_라운드_수를_반환한다() {
        assertThat(CardGameRound.ready(3).getTotalRounds()).isEqualTo(3);
    }
}
