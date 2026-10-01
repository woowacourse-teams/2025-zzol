package coffeeshout.admin.account.domain;

import coffeeshout.global.exception.custom.BusinessException;
import java.util.Optional;

/**
 * 관리자 이메일. 생성 시점에 정규화하고 검증한다.
 *
 * <p>허용목록 판정은 환경변수와 DB 두 곳에서 일어난다. 두 곳이 서로 다른 규칙으로 이메일을
 * 다루면 {@code Admin@Zzol.site}로 등록한 계정이 {@code admin@zzol.site}로 로그인할 때
 * 거부된다. 정규화를 타입 안으로 넣어 정규화되지 않은 값이 존재할 수 없게 만든다.
 */
public record AdminEmail(String value) {

    private static final int MAX_LENGTH = 255;

    public AdminEmail {
        value = normalize(value);
        validate(value);
    }

    /**
     * 형식이 어긋나면 예외를 던진다. 관리자가 직접 입력한 값처럼 틀렸다고 알려줘야 하는 자리에 쓴다.
     */
    public static AdminEmail of(String raw) {
        return new AdminEmail(raw);
    }

    /**
     * 형식이 어긋나면 빈 값을 돌려준다. 틀린 이유를 알려주면 안 되는 자리(로그인 판정)에 쓴다.
     */
    public static Optional<AdminEmail> parse(String raw) {
        try {
            return Optional.of(new AdminEmail(raw));
        } catch (BusinessException e) {
            return Optional.empty();
        }
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        final String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    private static void validate(String normalized) {
        if (normalized == null) {
            throw new BusinessException(AdminAccountErrorCode.INVALID_ADMIN_EMAIL, "관리자 이메일은 비어 있을 수 없습니다.");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new BusinessException(AdminAccountErrorCode.INVALID_ADMIN_EMAIL, "관리자 이메일이 255자를 넘습니다.");
        }
        // 완전한 RFC 검증은 하지 않는다. 실제 소유 검증은 구글 ID 토큰이 하므로
        // 여기서는 목록에 넣을 수 없는 값만 거른다.
        final int at = normalized.indexOf('@');
        if (at <= 0 || at == normalized.length() - 1 || normalized.indexOf('@', at + 1) >= 0) {
            throw new BusinessException(AdminAccountErrorCode.INVALID_ADMIN_EMAIL, "관리자 이메일 형식이 올바르지 않습니다.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
