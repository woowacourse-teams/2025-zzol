package coffeeshout.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class SeasonTierTest {

    @ParameterizedTest
    @CsvSource({
            "0, BRONZE",
            "299, BRONZE",
            "300, SILVER",
            "999, SILVER",
            "1000, GOLD",
            "2999, GOLD",
            "3000, DIAMOND",
            "100000, DIAMOND"
    })
    void 누적_포인트_임계로_티어를_판정한다(long totalPoints, SeasonTier expected) {
        assertThat(SeasonTier.fromPoints(totalPoints)).isEqualTo(expected);
    }

    @Test
    void 포인트가_음수면_예외가_발생한다() {
        assertThatThrownBy(() -> SeasonTier.fromPoints(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
