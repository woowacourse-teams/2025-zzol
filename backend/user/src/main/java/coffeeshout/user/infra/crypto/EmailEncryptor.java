package coffeeshout.user.infra.crypto;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.config.EmailCryptoProperties;
import coffeeshout.user.domain.UserErrorCode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * 이메일을 AES-256-GCM으로 가역 암호화한다. (원본 복원 가능)
 *
 * <p>저장 포맷: Base64({@code IV(12바이트) || ciphertext + GCM tag}). IV는 매 암호화마다 새로 생성하므로
 * 같은 평문도 암호문이 매번 달라진다 → 암호문으로는 동등 조회가 불가능하다(조회는 블라인드 인덱스 사용).
 *
 * <p>키는 설정값(고엔트로피 환경변수)을 SHA-256으로 해싱해 정확히 32바이트(AES-256) 키로 파생한다.
 */
@Component
public class EmailEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailEncryptor(EmailCryptoProperties properties) {
        this.secretKey = deriveKey(properties.encryptionKey());
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            final byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            final byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            final byte[] combined = ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new BusinessException(UserErrorCode.EMAIL_CRYPTO_FAILED, "이메일 암호화에 실패했습니다.");
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            final byte[] combined = Base64.getDecoder().decode(encoded);
            final byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            final byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new BusinessException(UserErrorCode.EMAIL_CRYPTO_FAILED, "이메일 복호화에 실패했습니다.");
        }
    }

    private SecretKey deriveKey(String rawKey) {
        try {
            final byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(UserErrorCode.EMAIL_CRYPTO_FAILED, "암호화 키 초기화에 실패했습니다.");
        }
    }
}
