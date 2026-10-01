package coffeeshout.global.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("LikePattern")
class LikePatternTest {

    @ParameterizedTest
    @CsvSource({
        "mj_admin, mj!_admin",
        "100%, 100!%",
        "a!b, a!!b",
        "_%!, !_!%!!",
        "보통글자, 보통글자",
    })
    void 와일드카드와_이스케이프_문자를_글자_그대로_바꾼다(String raw, String expected) {
        assertThat(LikePattern.escape(raw)).isEqualTo(expected);
    }

    @Test
    void 어디에_있든_걸리는_패턴으로_감싼다() {
        assertThat(LikePattern.contains("mj_admin")).isEqualTo("%mj!_admin%");
    }

    @Test
    void 빈_문자열은_전체를_거는_패턴이_된다() {
        // 비었을 때 조건을 걸지 말지는 부르는 쪽이 정한다. 여기서는 감싸기만 한다.
        assertThat(LikePattern.contains("")).isEqualTo("%%");
    }
}
