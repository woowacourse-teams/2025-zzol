package coffeeshout.global.ipblock;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 검증된 IP 주소 값 객체.
 * <p>
 * 생성 시점에 IPv4/IPv6 형식을 검증하므로, 이 타입을 받는 코드는
 * 로그·Redis 키에 값을 그대로 사용해도 안전하다 (parse, don't validate).
 * <ul>
 *   <li>{@link #Ip(String)} — 형식이 유효하지 않으면 {@link BusinessException}을 던진다 (어드민 입력 등 예외가 적절한 경계)</li>
 *   <li>{@link #tryFrom(String)} — 예외 대신 {@link Optional#empty()}를 반환한다 (필터처럼 조용히 건너뛰는 경계)</li>
 * </ul>
 */
public record Ip(String value) {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{0,4}(:[0-9a-fA-F]{0,4}){2,7}$"
    );

    public Ip {
        if (!isValid(value)) {
            throw new BusinessException(GlobalErrorCode.VALIDATION_ERROR, "유효하지 않은 IP 형식입니다.");
        }
    }

    public static Optional<Ip> tryFrom(String value) {
        if (!isValid(value)) {
            return Optional.empty();
        }
        return Optional.of(new Ip(value));
    }

    private static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return IPV4_PATTERN.matcher(value).matches() || IPV6_PATTERN.matcher(value).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}
