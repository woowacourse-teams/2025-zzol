package coffeeshout.room.domain.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TemperatureAvailabilityTest {

    @ParameterizedTest
    @ValueSource(strings = {"HOT_ONLY", "ICE_ONLY", "BOTH"})
    @DisplayName("올바른 문자열로 TemperatureAvailability를 생성한다")
    void 올바른_문자열로_TemperatureAvailability를_생성한다(String temperatureAvailability) {
        // when
        TemperatureAvailability result = TemperatureAvailability.from(temperatureAvailability);

        // then
        assertThat(result.name()).isEqualTo(temperatureAvailability);
    }

    @Test
    @DisplayName("잘못된 문자열로 TemperatureAvailability 생성시 예외가 발생한다")
    void 잘못된_문자열로_TemperatureAvailability_생성시_예외가_발생한다() {
        // given
        String invalidTemperatureAvailability = "INVALID";

        // when & then
        assertThatThrownBy(() -> TemperatureAvailability.from(invalidTemperatureAvailability))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 타입입니다.");
    }
}
