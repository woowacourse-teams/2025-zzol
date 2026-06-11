package coffeeshout.global.ipblock;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import io.netty.util.NetUtil;
import java.util.Optional;

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

    /**
     * 직접 작성한 정규식 대신 검증된 파서(Netty NetUtil)를 사용한다.
     * 정규식 기반 IPv6 검증은 압축 표기(::) 중복·IPv4-mapped 표기 등에서 오판하기 쉽고,
     * 필터 경로의 거짓 거부는 곧 차단 우회로 이어진다.
     */
    private static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return NetUtil.isValidIpV4Address(value) || NetUtil.isValidIpV6Address(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
