package coffeeshout.room.domain.roulette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProbabilityCalculatorTest {

    @Test
    void 정상적인_파라미터로_생성자를_호출하면_객체가_정상적으로_생성된다() {
        // given & when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThatCode(() -> new ProbabilityCalculator(2, 1)).doesNotThrowAnyException();
            softly.assertThatCode(() -> new ProbabilityCalculator(4, 5)).doesNotThrowAnyException();
            softly.assertThatCode(() -> new ProbabilityCalculator(6, 10)).doesNotThrowAnyException();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1, Integer.MIN_VALUE})
    void 플레이어_수가_2보다_작으면_예외가_발생한다(int invalidPlayerCount) {
        assertThatThrownBy(() -> new ProbabilityCalculator(invalidPlayerCount, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5, Integer.MIN_VALUE})
    void 라운드_수가_0보다_작거나_같으면_예외가_발생한다(int invalidRoundCount) {
        assertThatThrownBy(() -> new ProbabilityCalculator(4, invalidRoundCount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 홀수_플레이어에서_중간등수는_확률_변화가_0이다() {
        // given
        ProbabilityCalculator calculator = new ProbabilityCalculator(5, 10);

        // when
        int probabilityChange = calculator.calculateProbabilityChange(3, 1); // 5명에서 3등은 UNDECIDED

        // then
        assertThat(probabilityChange).isEqualTo(0);
    }

    @Test
    void 동점자가_여러명일때_평균_확률_변화를_계산한다() {
        // given
        ProbabilityCalculator calculator = new ProbabilityCalculator(4, 5);

        // when
        int probabilityChangeFor2_3Tie = calculator.calculateProbabilityChange(2, 2);

        // then
        assertThat(probabilityChangeFor2_3Tie).isEqualTo(0); // (-350 + 350) / 2 = 0
    }

    @Test
    void 최소_플레이어_수_2명에서_정상_동작한다() {
        // given
        ProbabilityCalculator calculator = new ProbabilityCalculator(2, 1);

        // when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(calculator.calculateProbabilityChange(1, 1))
                    .isEqualTo((int) (-5000 * ProbabilityCalculator.ADJUSTMENT_WEIGHT)); // 2명에서 1등
            softly.assertThat(calculator.calculateProbabilityChange(2, 1))
                    .isEqualTo((int) (5000 * ProbabilityCalculator.ADJUSTMENT_WEIGHT));  // 2명에서 2등
        });
    }

    @Test
    void 전체_플레이어가_모두_동점일_때_확률_변화의_합은_0이다() {
        // given
        ProbabilityCalculator calculator = new ProbabilityCalculator(6, 5);

        // when
        int totalChange = 0;
        for (int rank = 1; rank <= 6; rank++) {
            totalChange += calculator.calculateProbabilityChange(rank, 1);
        }

        // then
        assertThat(totalChange).isZero(); // 전체 확률 변화의 합은 0이어야 함
    }
}
