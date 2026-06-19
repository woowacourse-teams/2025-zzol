package coffeeshout.user.infra.crypto;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.config.EmailCryptoProperties;
import coffeeshout.user.domain.UserErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * 이메일의 블라인드 인덱스(검색용 결정적 해시)를 생성한다.
 *
 * <p>이메일을 {@code trim().toLowerCase()}로 정규화한 뒤 HMAC-SHA256으로 해싱한다. 같은 이메일은 항상
 * 같은 해시를 내므로 {@code WHERE email_hash = ?} 동등 조회에 사용한다. 단방향이라 원본 복원은 불가능하다.
 * 원본 보관은 {@link EmailEncryptor}가 담당한다.
 */
@Component
public class EmailBlindIndexHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public EmailBlindIndexHasher(EmailCryptoProperties properties) {
        this.key = new SecretKeySpec(properties.hmacKey().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String hash(String email) {
        if (email == null) {
            return null;
        }
        final String normalized = email.trim().toLowerCase(Locale.ROOT);
        try {
            final Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            final byte[] digest = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new BusinessException(UserErrorCode.EMAIL_CRYPTO_FAILED, "이메일 해시 생성에 실패했습니다.");
        }
    }
}
