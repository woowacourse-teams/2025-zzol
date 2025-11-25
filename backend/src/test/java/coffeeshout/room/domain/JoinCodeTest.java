package coffeeshout.room.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JoinCodeTest {

    @ParameterizedTest
    @ValueSource(strings = {"ABCD", "AXBC"})
    void 조인코드는_규칙에_맞게_생성된다(String address) {
        // given
        JoinCode result = new JoinCode(address);

        // when & then
        assertThat(result).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABCDE", "A1LB2", "#ABCD", "EXSD"})
    void 조인코드가_규칙에_맞지_않는다면_예외를_발생한다(String address) {
        // given
        // when & then
        assertThatThrownBy(() -> new JoinCode(address))
                .isInstanceOf(InvalidArgumentException.class);
    }
}
