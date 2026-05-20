package coffeeshout.room.domain.roulette;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.room.domain.RoomErrorCode;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProbabilityCalculatorTest {

    @Test
    void 정상적인_파라미터로_생성자를_호출하면_객체가_정상적으로_생성된다() {
        // given & when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThatCode(() -> new ProbabilityCalculator(2, 1, 0.7)).doesNotThrowAnyException();
            softly.assertThatCode(() -> new ProbabilityCalculator(4, 5, 0.7)).doesNotThrowAnyException();
            softly.assertThatCode(() -> new ProbabilityCalculator(6, 10, 0.7)).doesNotThrowAnyException();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1, Integer.MIN_VALUE})
    void 플레이어_수가_2보다_작으면_예외가_발생한다(int invalidPlayerCount) {
        assertCoffeeShoutException(
                () -> new ProbabilityCalculator(invalidPlayerCount, 5, 0.7),
                RoomErrorCode.INSUFFICIENT_PLAYER_COUNT
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5, Integer.MIN_VALUE})
    void 라운드_수가_0보다_작거나_같으면_예외가_발생한다(int invalidRoundCount) {
        assertCoffeeShoutException(
                () -> new ProbabilityCalculator(4, invalidRoundCount, 0.7),
                RoomErrorCode.INVALID_ROUND_COUNT
        );
    }

    @Test
    void 홀수_플레이어에서_중간등수는_확률_변화가_0이다() {
        // given
        ProbabilityCalculator calculator = new ProbabilityCalculator(5, 10, 0.7);

        // when
        int probabilityChange = calculator.calculateProbabilityChange(3, 1); // 5명에서 3등은 UNDECIDED

        // then
        assertThat(probabilityChange).isZero();
    }

    @Test
    void 동점자가_여러명일때_평균_확률_변화를_계산한다() {
        // given
        ProbabilityCalculator calculator = new ProbabilityCalculator(4, 5, 0.7);

        // when
        int probabilityChangeFor2_3Tie = calculator.calculateProbabilityChange(2, 2);

        // then
        assertThat(probabilityChangeFor2_3Tie).isZero(); // (-350 + 350) / 2 = 0
    }

    @Test
    void 최소_플레이어_수_2명에서_정상_동작한다() {
        // given
        ProbabilityCalculator calculator = new ProbabilityCalculator(2, 1, 0.7);

        // when & then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(calculator.calculateProbabilityChange(1, 1))
                    .isEqualTo((int) (-5000 * 0.7)); // 2명에서 1등
            softly.assertThat(calculator.calculateProbabilityChange(2, 1))
                    .isEqualTo((int) (5000 * 0.7));  // 2명에서 2등
        });
    }

    @Test
    void 전체_플레이어가_모두_동점일_때_확률_변화의_합은_0이다() {
        // given
        ProbabilityCalculator calculator = new ProbabilityCalculator(6, 5, 0.7);

        // when
        int totalChange = 0;
        for (int rank = 1; rank <= 6; rank++) {
            totalChange += calculator.calculateProbabilityChange(rank, 1);
        }

        // then
        assertThat(totalChange).isZero(); // 전체 확률 변화의 합은 0이어야 함
    }

    @Nested
    class 가중치_유효성_검증 {

        @Test
        void 경계값_0_1과_0_9는_정상_생성된다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThatCode(() -> new ProbabilityCalculator(4, 5, 0.1)).doesNotThrowAnyException();
                softly.assertThatCode(() -> new ProbabilityCalculator(4, 5, 0.9)).doesNotThrowAnyException();
            });
        }

        @Test
        void 가중치가_클수록_확률_변화량이_커진다() {
            // given
            ProbabilityCalculator lowWeight = new ProbabilityCalculator(2, 1, 0.1);
            ProbabilityCalculator highWeight = new ProbabilityCalculator(2, 1, 0.9);

            // when
            int lowChange = Math.abs(lowWeight.calculateProbabilityChange(1, 1));
            int highChange = Math.abs(highWeight.calculateProbabilityChange(1, 1));

            // then
            assertThat(highChange).isGreaterThan(lowChange);
        }
    }
}
