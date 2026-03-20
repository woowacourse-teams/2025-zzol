package coffeeshout.room.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.room.application.service.nickname.NicknameValidator;
import coffeeshout.room.domain.RoomErrorCode;
import com.vane.badwordfiltering.BadWordFiltering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NicknameValidatorTest {

    NicknameValidator nicknameValidator;

    @BeforeEach
    void setUp() {
        BadWordFiltering filtering = new BadWordFiltering();
        filtering.add("shit");
        filtering.add("ass");
        filtering.add("fuck");
        filtering.add("bastard");
        nicknameValidator = new NicknameValidator(filtering);
    }

    @Nested
    class 정상_닉네임 {

        @ParameterizedTest
        @ValueSource(strings = {"용감한호랑이", "player1", "게스트", "홍길동", "빠른여우"})
        void 정상_닉네임은_검증을_통과한다(String nickname) {
            assertThatCode(() -> nicknameValidator.validate(nickname))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class 비속어_닉네임 {

        @ParameterizedTest
        @ValueSource(strings = {"씨발", "개새끼", "ㅅㅂ"})
        void 직접_비속어는_예외를_발생한다(String nickname) {
            assertThatThrownBy(() -> nicknameValidator.validate(nickname))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY);
        }

        @ParameterizedTest
        @ValueSource(strings = {"씨 발", "개 새끼"})
        void 공백_삽입으로_우회한_비속어는_예외를_발생한다(String nickname) {
            assertThatThrownBy(() -> nicknameValidator.validate(nickname))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY);
        }

        @ParameterizedTest
        @ValueSource(strings = {"씨@@@@@발", "씨!!!!!!발", "개#새#끼", "씨*발", "씨.발"})
        void 특수문자_삽입으로_우회한_한국어_비속어는_예외를_발생한다(String nickname) {
            assertThatThrownBy(() -> nicknameValidator.validate(nickname))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY);
        }

        @ParameterizedTest
        @ValueSource(strings = {"sh1t", "sh!t", "@ss", "$hit", "b1tch", "4ss"})
        void 리트스피크로_우회한_영어_비속어는_예외를_발생한다(String nickname) {
            assertThatThrownBy(() -> nicknameValidator.validate(nickname))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY);
        }
    }
}
