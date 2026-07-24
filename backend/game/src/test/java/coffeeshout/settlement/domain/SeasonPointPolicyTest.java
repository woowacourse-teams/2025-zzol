package coffeeshout.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SeasonPointPolicyTest {

    @ParameterizedTest
    @CsvSource({"1, 100", "2, 70", "3, 50", "4, 30", "8, 30"})
    void 순위에_따라_포인트를_환산한다(int rank, int expectedPoints) {
        assertThat(SeasonPointPolicy.pointsFor(rank)).isEqualTo(expectedPoints);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void 순위가_1_미만이면_예외가_발생한다(int rank) {
        assertThatThrownBy(() -> SeasonPointPolicy.pointsFor(rank))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
