package coffeeshout.user.infra.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.user.config.EmailCryptoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmailBlindIndexHasherTest {

    private static final String ENCRYPTION_KEY = "test-email-encryption-key-at-least-32-chars";
    private static final String HMAC_KEY = "test-email-hmac-key-at-least-32-characters";

    private EmailBlindIndexHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new EmailBlindIndexHasher(new EmailCryptoProperties(ENCRYPTION_KEY, HMAC_KEY));
    }

    @Nested
    class 해시_생성 {

        @Test
        void 같은_이메일은_항상_같은_해시를_생성한다() {
            final String email = "user@example.com";

            assertThat(hasher.hash(email)).isEqualTo(hasher.hash(email));
        }

        @Test
        void 대소문자와_공백은_정규화되어_같은_해시를_생성한다() {
            final String hashed = hasher.hash("user@example.com");

            assertThat(hasher.hash("  USER@Example.COM  ")).isEqualTo(hashed);
        }

        @Test
        void 다른_이메일은_다른_해시를_생성한다() {
            assertThat(hasher.hash("a@example.com")).isNotEqualTo(hasher.hash("b@example.com"));
        }

        @Test
        void HMAC_SHA256_해시는_64자_hex_문자열이다() {
            final String hashed = hasher.hash("user@example.com");

            assertThat(hashed).hasSize(64).matches("[0-9a-f]+");
        }

        @Test
        void 다른_HMAC_키는_다른_해시를_생성한다() {
            final EmailBlindIndexHasher otherHasher = new EmailBlindIndexHasher(
                    new EmailCryptoProperties(ENCRYPTION_KEY, "another-hmac-key-at-least-32-characters-long"));

            assertThat(otherHasher.hash("user@example.com")).isNotEqualTo(hasher.hash("user@example.com"));
        }

        @Test
        void null은_그대로_null로_처리한다() {
            assertThat(hasher.hash(null)).isNull();
        }
    }
}
