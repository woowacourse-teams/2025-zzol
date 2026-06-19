package coffeeshout.user.infra.crypto;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.user.config.EmailCryptoProperties;
import coffeeshout.user.domain.UserErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmailEncryptorTest {

    private static final String ENCRYPTION_KEY = "test-email-encryption-key-at-least-32-chars";
    private static final String HMAC_KEY = "test-email-hmac-key-at-least-32-characters";

    private EmailEncryptor emailEncryptor;

    @BeforeEach
    void setUp() {
        emailEncryptor = new EmailEncryptor(new EmailCryptoProperties(ENCRYPTION_KEY, HMAC_KEY));
    }

    @Nested
    class 암호화_복호화 {

        @Test
        void 암호화한_이메일을_복호화하면_원본이_복원된다() {
            final String email = "user@example.com";

            final String encrypted = emailEncryptor.encrypt(email);
            final String decrypted = emailEncryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(email);
        }

        @Test
        void 암호문은_평문과_다르다() {
            final String email = "user@example.com";

            final String encrypted = emailEncryptor.encrypt(email);

            assertThat(encrypted).isNotEqualTo(email);
        }

        @Test
        void 같은_이메일도_매번_다른_암호문이_생성된다() {
            final String email = "user@example.com";

            final String first = emailEncryptor.encrypt(email);
            final String second = emailEncryptor.encrypt(email);

            assertThat(first).isNotEqualTo(second);
            assertThat(emailEncryptor.decrypt(first)).isEqualTo(email);
            assertThat(emailEncryptor.decrypt(second)).isEqualTo(email);
        }

        @Test
        void null은_그대로_null로_처리한다() {
            assertThat(emailEncryptor.encrypt(null)).isNull();
            assertThat(emailEncryptor.decrypt(null)).isNull();
        }

        @Test
        void 다른_키로_암호화한_값은_복호화에_실패한다() {
            final String encrypted = emailEncryptor.encrypt("user@example.com");
            final EmailEncryptor otherEncryptor = new EmailEncryptor(
                    new EmailCryptoProperties("another-encryption-key-at-least-32-chars-long", HMAC_KEY));

            assertCoffeeShoutException(
                    () -> otherEncryptor.decrypt(encrypted),
                    UserErrorCode.EMAIL_CRYPTO_FAILED
            );
        }
    }
}
