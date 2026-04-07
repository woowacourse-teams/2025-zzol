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
        if (xForwardedFor == null || xForwardedFor.isBlank() || "unknown".equalsIgnoreCase(xForwardedFor)) {
            return request.getRemoteAddr();
        }
        final String[] ips = xForwardedFor.split(",");
        return ips[ips.length - 1].trim();
    }
}
