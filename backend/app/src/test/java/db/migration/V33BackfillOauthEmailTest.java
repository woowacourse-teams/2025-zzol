package db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.user.config.EmailCryptoProperties;
import coffeeshout.user.infra.crypto.EmailBlindIndexHasher;
import coffeeshout.user.infra.crypto.EmailEncryptor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class V33BackfillOauthEmailTest {

    private static final String ENCRYPTION_KEY = "test-email-encryption-key-at-least-32-chars";
    private static final String HMAC_KEY = "test-email-hmac-key-at-least-32-characters";

    private Connection connection;
    private EmailEncryptor encryptor;
    private EmailBlindIndexHasher hasher;
    private V33__backfill_oauth_email migration;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:v33test;DB_CLOSE_DELAY=-1", "sa", "");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE oauth_account (
                        id BIGINT PRIMARY KEY,
                        email VARCHAR(512),
                        email_hash VARCHAR(64)
                    )
                    """);
        }
        final EmailCryptoProperties properties = new EmailCryptoProperties(ENCRYPTION_KEY, HMAC_KEY);
        encryptor = new EmailEncryptor(properties);
        hasher = new EmailBlindIndexHasher(properties);
        migration = new V33__backfill_oauth_email();
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE oauth_account");
        }
        connection.close();
    }

    private void insert(long id, String email, String emailHash) throws Exception {
        try (Statement statement = connection.createStatement()) {
            final String emailValue = email == null ? "NULL" : "'" + email + "'";
            final String hashValue = emailHash == null ? "NULL" : "'" + emailHash + "'";
            statement.execute(
                    "INSERT INTO oauth_account (id, email, email_hash) VALUES (%d, %s, %s)"
                            .formatted(id, emailValue, hashValue));
        }
    }

    private String column(long id, String name) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "SELECT %s FROM oauth_account WHERE id = %d".formatted(name, id))) {
            rs.next();
            return rs.getString(name);
        }
    }

    @Nested
    class 평문_이메일_백필 {

        @Test
        void 평문_이메일을_암호문으로_바꾸고_해시를_채운다() throws Exception {
            insert(1L, "user@example.com", null);

            migration.backfill(connection, encryptor, hasher);

            final String storedEmail = column(1L, "email");
            assertThat(storedEmail).isNotEqualTo("user@example.com");
            assertThat(encryptor.decrypt(storedEmail)).isEqualTo("user@example.com");
            assertThat(column(1L, "email_hash")).isEqualTo(hasher.hash("user@example.com"));
        }

        @Test
        void 이미_해시가_있는_행은_건드리지_않는다() throws Exception {
            insert(1L, "already-encrypted", "existing-hash");

            migration.backfill(connection, encryptor, hasher);

            assertThat(column(1L, "email")).isEqualTo("already-encrypted");
            assertThat(column(1L, "email_hash")).isEqualTo("existing-hash");
        }

        @Test
        void 이메일이_없는_행은_건너뛴다() throws Exception {
            insert(1L, null, null);

            migration.backfill(connection, encryptor, hasher);

            assertThat(column(1L, "email")).isNull();
            assertThat(column(1L, "email_hash")).isNull();
        }
    }
}
