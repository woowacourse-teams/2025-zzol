package coffeeshout.admin.auth.domain;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 관리자 refresh 토큰. 쿠키에는 {@code {familyId}.{tokenId}} 로 담긴다.
 *
 * <p>family 는 로그인 한 번이다. 재발급할 때마다 tokenId 만 바뀌고 familyId 는 유지된다.
 * 재사용이 감지되면 family 를 통째로 지워 그 로그인만 끊는다. 같은 관리자의 다른 기기는 살아 있다.
 *
 * <p>둘 다 128비트 난수를 base64url 로 쓴다. 그 문자 집합에는 {@code .} 이 없어 구분자로 안전하다.
 */
public record AdminRefreshToken(String familyId, String tokenId) {

    private static final int RANDOM_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    // 길이 상한을 둔다. 쿠키 값이 그대로 Redis 키가 되므로, 부르는 쪽이 긴 값을 넣어 키를 불리지 못하게 한다.
    private static final Pattern FORMAT = Pattern.compile("^([A-Za-z0-9_-]{16,64})\\.([A-Za-z0-9_-]{16,64})$");

    public static AdminRefreshToken newFamily() {
        return new AdminRefreshToken(randomId(), randomId());
    }

    /**
     * 형식이 어긋나면 빈 값을 돌려준다. 어디가 틀렸는지 알려줄 이유가 없다.
     */
    public static Optional<AdminRefreshToken> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        final Matcher matcher = FORMAT.matcher(raw);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new AdminRefreshToken(matcher.group(1), matcher.group(2)));
    }

    /** 같은 family 의 다음 토큰. */
    public AdminRefreshToken rotate() {
        return new AdminRefreshToken(familyId, randomId());
    }

    public String value() {
        return familyId + "." + tokenId;
    }

    private static String randomId() {
        final byte[] bytes = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
