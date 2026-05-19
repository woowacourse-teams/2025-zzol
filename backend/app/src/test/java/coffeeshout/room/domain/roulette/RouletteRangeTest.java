package coffeeshout.room.domain.roulette;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RouletteRangeTest {

    @ParameterizedTest
    @ValueSource(ints = {10, 15, 20})
    void 숫자가_범위에_포함되면_true를_반환한다(int number) {
        // given
        RouletteRange range = new RouletteRange(10, 20, PlayerFixture.호스트한스());

        // when
        boolean result = range.isBetween(number);

        // then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {9, 21, -100, 100})
    void 숫자가_범위에_포함되지_않으면_false를_반환한다(int number) {
        // given
        RouletteRange range = new RouletteRange(10, 20, PlayerFixture.호스트한스());

        // when
        boolean result = range.isBetween(number);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 시작값과_끝값도_범위에_포함된다() {
        // given
        RouletteRange range = new RouletteRange(10, 20, PlayerFixture.호스트한스());

        // when & then
        assertThat(range.isBetween(10)).isTrue();
        assertThat(range.isBetween(20)).isTrue();
    }
}
