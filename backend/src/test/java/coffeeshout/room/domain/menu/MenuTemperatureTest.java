package coffeeshout.room.domain.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MenuTemperatureTest {

    @ParameterizedTest
    @ValueSource(strings = {"HOT", "ICE"})
    @DisplayName("올바른 문자열로 MenuTemperature를 생성한다")
    void 올바른_문자열로_MenuTemperature를_생성한다(String temperature) {
        // when
        MenuTemperature result = MenuTemperature.from(temperature);

        // then
        assertThat(result.name()).isEqualTo(temperature);
    }

    @Test
    @DisplayName("잘못된 문자열로 MenuTemperature 생성시 예외가 발생한다")
    void 잘못된_문자열로_MenuTemperature_생성시_예외가_발생한다() {
        // given
        String invalidTemperature = "WARM";

        // when & then
        assertThatThrownBy(() -> MenuTemperature.from(invalidTemperature))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
