package coffeeshout.room.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.PlayerNameValidator;
import coffeeshout.room.domain.service.ProfanityChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerNameValidatorTest {

    ProfanityChecker profanityChecker;
    PlayerNameValidator playerNameValidator;

    @BeforeEach
    void setUp() {
        profanityChecker = mock(ProfanityChecker.class);
        playerNameValidator = new PlayerNameValidator(profanityChecker);
    }

    @Nested
    class null_또는_blank인_경우 {

        @Test
        void null_PlayerName은_NullPointerException을_던진다() {
            assertThatThrownBy(() -> playerNameValidator.validate((PlayerName) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void null_이름은_예외를_던진다() {
            assertThatThrownBy(() -> playerNameValidator.validate(new PlayerName(null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_BLANK);
        }

        @Test
        void 빈_문자열_이름은_예외를_던진다() {
            assertThatThrownBy(() -> playerNameValidator.validate(new PlayerName("")))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_BLANK);
        }

        @Test
        void 공백만_있는_이름은_예외를_던진다() {
            assertThatThrownBy(() -> playerNameValidator.validate(new PlayerName("   ")))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_BLANK);
        }
    }

    @Nested
    class 비속어가_없는_경우 {

        @Test
        void 예외_없이_통과한다() {
            when(profanityChecker.contains("용감한호랑이")).thenReturn(false);

            assertThatCode(() -> playerNameValidator.validate(new PlayerName("용감한호랑이")))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class 비속어가_있는_경우 {

        @Test
        void BusinessException을_던진다() {
            when(profanityChecker.contains("비속어닉네임")).thenReturn(true);

            assertThatThrownBy(() -> playerNameValidator.validate(new PlayerName("비속어닉네임")))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY);
        }
    }
}
