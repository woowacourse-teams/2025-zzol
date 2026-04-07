package coffeeshout.global.filter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * HTTP 요청에서 클라이언트 IP를 추출하는 유틸리티.
 * <p>
 * {@code X-Forwarded-For} 헤더가 있으면 마지막 IP를 반환하고,
 * 없거나 유효하지 않으면 {@code request.getRemoteAddr()}를 반환한다.
 */
public final class ClientIpExtractor {

    private ClientIpExtractor() {
    }

    public static String extract(HttpServletRequest request) {
        final String xForwardedFor = request.getHeader("X-Forwarded-For");
        // 신뢰 경계가 설정된 환경에서만 사용, 마지막 토큰 검증
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }
        final String[] ips = xForwardedFor.split(",");
        final String candidate = ips[ips.length - 1].trim();
        if (candidate.isBlank() || "unknown".equalsIgnoreCase(candidate)) {
            return request.getRemoteAddr();
        }
        return candidate;
    }
}
