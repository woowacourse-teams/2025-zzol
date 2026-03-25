package coffeeshout.room.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.vane.badwordfiltering.BadWordFiltering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VaneProfanityCheckerTest {

    VaneProfanityChecker profanityChecker;

    @BeforeEach
    void setUp() {
        profanityChecker = new VaneProfanityChecker(new BadWordFiltering());
    }

    @Nested
    class 정상_닉네임 {

        @ParameterizedTest
        @ValueSource(strings = {"용감한호랑이", "player1", "게스트", "홍길동", "빠른여우"})
        void 비속어가_없으면_false를_반환한다(String nickname) {
            assertThat(profanityChecker.contains(nickname)).isFalse();
        }
    }

    @Nested
    class 직접_비속어 {

        @ParameterizedTest
        @ValueSource(strings = {"씨발", "병신"})
        void 직접_비속어는_true를_반환한다(String nickname) {
            assertThat(profanityChecker.contains(nickname)).isTrue();
        }
    }

    @Nested
    class 공백_삽입_우회 {

        @ParameterizedTest
        @ValueSource(strings = {"씨 발", "병 신"})
        void 공백_삽입으로_우회한_비속어는_true를_반환한다(String nickname) {
            assertThat(profanityChecker.contains(nickname)).isTrue();
        }
    }

    @Nested
    class 자음_축약_우회 {

        @ParameterizedTest
        @ValueSource(strings = {"ㅅㅂ", "ㅂㅅ"})
        void 자음_축약으로_우회한_비속어는_true를_반환한다(String nickname) {
            assertThat(profanityChecker.contains(nickname)).isTrue();
        }
    }

    @Nested
    class 특수문자_삽입_우회 {

        @ParameterizedTest
        @ValueSource(strings = {"씨.발", "씨_발", "씨!발"})
        void 특수문자_삽입으로_우회한_비속어는_true를_반환한다(String nickname) {
            assertThat(profanityChecker.contains(nickname)).isTrue();
        }
    }

    @Nested
    class 숫자_삽입_우회 {

        @ParameterizedTest
        @ValueSource(strings = {"씨1발"})
        void 숫자_삽입으로_우회한_비속어는_true를_반환한다(String nickname) {
            assertThat(profanityChecker.contains(nickname)).isTrue();
        }
    }
}
