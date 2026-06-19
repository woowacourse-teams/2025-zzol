package db.migration;

import coffeeshout.user.config.EmailCryptoProperties;
import coffeeshout.user.infra.crypto.EmailBlindIndexHasher;
import coffeeshout.user.infra.crypto.EmailEncryptor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * V32에서 추가된 암호화 컬럼 정책에 맞춰, 기존 평문 이메일을 일괄 암호화 + 해시로 전환한다.
 *
 * <p>V32로 컨버터가 적용되면 기존 평문 행을 JPA로 읽을 때 GCM 복호화가 평문에 대해 실패한다.
 * Flyway는 앱 기동 전에 실행되므로, 이 마이그레이션으로 평문 행을 모두 전환해 "복호화 실패" 구간을 없앤다.
 *
 * <p>SQL로는 AES/HMAC 연산이 불가능하므로 Java 마이그레이션으로 작성한다. 키는 앱과 동일한 환경변수에서
 * 읽는다. {@code email_hash IS NULL AND email IS NOT NULL} 대상만 처리하므로 재실행해도 안전(멱등)하다.
 */
public class V33__backfill_oauth_email extends BaseJavaMigration {

    private static final String SELECT_PLAINTEXT =
            "SELECT id, email FROM oauth_account WHERE email_hash IS NULL AND email IS NOT NULL";
    private static final String UPDATE_ENCRYPTED =
            "UPDATE oauth_account SET email = ?, email_hash = ? WHERE id = ?";
    private static final int MIN_KEY_LENGTH = 32;

    @Override
    public void migrate(Context context) throws Exception {
        final EmailCryptoProperties properties = loadProperties();
        backfill(context.getConnection(), new EmailEncryptor(properties), new EmailBlindIndexHasher(properties));
    }

    /**
     * 평문 이메일 행을 읽어 암호화 + 해시로 업데이트한다. 환경변수에 의존하지 않아 테스트에서 직접 호출할 수 있다.
     */
    void backfill(Connection connection, EmailEncryptor encryptor, EmailBlindIndexHasher hasher) throws Exception {
        final List<Long> ids = new ArrayList<>();
        final List<String> plaintexts = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(SELECT_PLAINTEXT);
                ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                ids.add(rows.getLong("id"));
                plaintexts.add(rows.getString("email"));
            }
        }

        if (ids.isEmpty()) {
            return;
        }

        try (PreparedStatement update = connection.prepareStatement(UPDATE_ENCRYPTED)) {
            for (int i = 0; i < ids.size(); i++) {
                final String plaintext = plaintexts.get(i);
                update.setString(1, encryptor.encrypt(plaintext));
                update.setString(2, hasher.hash(plaintext));
                update.setLong(3, ids.get(i));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private EmailCryptoProperties loadProperties() {
        // EmailCryptoProperties의 @Size 검증은 Spring 바인딩 시에만 동작하고, 여기서는 직접 생성하므로 우회된다.
        // 약한 키로 백필되는 것을 막기 위해 최소 길이를 직접 검증한다.
        final String encryptionKey = requireKey("USER_EMAIL_ENCRYPTION_KEY", System.getenv("USER_EMAIL_ENCRYPTION_KEY"));
        final String hmacKey = requireKey("USER_EMAIL_HMAC_KEY", System.getenv("USER_EMAIL_HMAC_KEY"));
        return new EmailCryptoProperties(encryptionKey, hmacKey);
    }

    private String requireKey(String name, String value) {
        if (value == null || value.length() < MIN_KEY_LENGTH) {
            throw new IllegalStateException(
                    "평문 이메일 백필을 위해 %s 환경변수가 %d자 이상으로 설정되어야 합니다.".formatted(name, MIN_KEY_LENGTH));
        }
        return value;
    }
}
