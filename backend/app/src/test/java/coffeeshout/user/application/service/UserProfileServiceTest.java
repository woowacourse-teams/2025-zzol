package coffeeshout.user.application.service;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.UserFixture;
import coffeeshout.support.app.ServiceTest;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserErrorCode;
import coffeeshout.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserProfileServiceTest extends ServiceTest {

    @Autowired
    UserProfileService userProfileService;

    @Autowired
    UserRepository userRepository;

    Long userId;

    @BeforeEach
    void setUp() {
        userId = userRepository.save(UserFixture.회원_엠제이()).getId();
    }

    @Nested
    class 닉네임_변경 {

        @Test
        void 비속어가_포함된_닉네임은_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> userProfileService.changeNickname(userId, "씨발"),
                    UserErrorCode.NICKNAME_CONTAINS_PROFANITY
            );
        }

        @Test
        void 정상_닉네임은_변경에_성공한다() {
            final User updated = userProfileService.changeNickname(userId, "새닉네임");

            assertThat(updated.getNickname().value()).isEqualTo("새닉네임");
        }
    }
}
