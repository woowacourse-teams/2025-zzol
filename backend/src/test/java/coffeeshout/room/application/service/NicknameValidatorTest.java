package coffeeshout.room.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.room.application.service.nickname.NicknameValidator;
import coffeeshout.room.domain.RoomErrorCode;
import com.vane.badwordfiltering.BadWordFiltering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NicknameValidatorTest {

    NicknameValidator nicknameValidator;

    @BeforeEach
    void setUp() {
        nicknameValidator = new NicknameValidator(new BadWordFiltering());
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

        @Test
        void 직접_비속어는_예외를_발생한다() {
            assertThatThrownBy(() -> nicknameValidator.validate("씨발"))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY);
        }

        @Test
        void 공백_삽입으로_우회한_비속어는_예외를_발생한다() {
            assertThatThrownBy(() -> nicknameValidator.validate("씨 발"))
                    .isInstanceOf(InvalidArgumentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY);
        }

    }
}
