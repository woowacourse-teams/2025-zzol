package coffeeshout.user.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.profanity.domain.ProfanityChecker;
import coffeeshout.user.domain.UserErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NicknameValidatorTest {

    ProfanityChecker profanityChecker;
    NicknameValidator nicknameValidator;

    @BeforeEach
    void setUp() {
        profanityChecker = mock(ProfanityChecker.class);
        nicknameValidator = new NicknameValidator(profanityChecker);
    }

    @Nested
    class 비속어가_없는_경우 {

        @Test
        void 예외_없이_통과한다() {
            when(profanityChecker.contains("용감한호랑이")).thenReturn(false);

            assertThatCode(() -> nicknameValidator.validate("용감한호랑이"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class 비속어가_있는_경우 {

        @Test
        void NICKNAME_CONTAINS_PROFANITY_예외를_던진다() {
            when(profanityChecker.contains("비속어닉네임")).thenReturn(true);

            assertThatThrownBy(() -> nicknameValidator.validate("비속어닉네임"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NICKNAME_CONTAINS_PROFANITY);
        }
    }
}
