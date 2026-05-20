package coffeeshout.user.infra.persistence;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.mockito.Mockito.mock;

import coffeeshout.exception.GlobalErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserEntityTest {

    @Nested
    class toDomain변환 {

        @Test
        void 탈퇴한_사용자는_도메인_객체로_변환할_수_없다() {
            final UserEntity entity = new UserEntity("AB3CD", "엠제이");
            entity.anonymize();
            entity.softDelete();

            assertCoffeeShoutException(
                    () -> entity.toDomain(mock(OAuthAccountEntity.class)),
                    GlobalErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }
}
