package coffeeshout.global.ipblock;

import coffeeshout.global.exception.GlobalErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IP 기반 차단 필터.
 * <p>
 * 세 가지 차단 정책을 적용한다:
 * <ol>
 *   <li>악성 경로(스캐너 패턴) 접근 시 해당 IP를 즉시 차단하고 403 반환 (예외 경로 포함 — 항상 적용)</li>
 *   <li>이미 차단된 IP는 요청 초입에서 즉시 403 반환 (예외 경로는 건너뜀)</li>
 *   <li>404 응답이 누적되면 차단 (IpBlockStore 위임)</li>
 * </ol>
 *
 * <p>예외 경로({@code security.ip-block.exempt-paths})는 차단 여부 검사를 우회하지만,
 * 악성 경로 패턴에 해당하면 예외 없이 즉시 차단한다.
 * Spring Security가 해당 경로의 인증·인가를 2차로 담당한다.
 *
 * <p>{@code HIGHEST_PRECEDENCE}로 등록해 Security 필터 체인보다 먼저 실행한다.
 * 차단된 요청은 Security 처리 비용 없이 즉시 반환된다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class IpBlockFilter extends OncePerRequestFilter {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{0,4}(:[0-9a-fA-F]{0,4}){2,7}$"
    );

    private final IpBlockStore ipBlockStore;
    private final MaliciousPathMatcher maliciousPathMatcher;
    private final IpBlockProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String uri = request.getRequestURI();
        final String ip = getClientIp(request);

        if (!isValidIp(ip)) {
            log.debug("유효하지 않은 IP 형식, 필터 처리 건너뜀: ip={}", ip);
            filterChain.doFilter(request, response);
            return;
        }

        // 사설/내부 IP(프록시·헬스체크·내부 서비스)는 클라이언트가 아니므로 차단·카운트 대상에서 제외한다.
        // XFF가 깨지면 모든 트래픽이 프록시 IP로 보이는데, 이를 차단하면 전체 장애로 번진다.
        // 악성 경로 접근은 XFF 설정 이상 신호이므로 경고만 남기고 통과시킨다.
        if (isInternalIp(ip)) {
            if (maliciousPathMatcher.isMalicious(uri)) {
                ipBlockStore.recordInternalIpSuspicious(ip, uri);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // 악성 경로는 예외 경로여도 항상 차단 (/admin.php 등 스캐너 패턴)
        if (maliciousPathMatcher.isMalicious(uri)) {
            log.warn("악성 경로 접근 감지 → IP 즉시 차단: ip={} uri={}", ip, uri);
            ipBlockStore.blockImmediately(ip);
            writeBlockedResponse(request, response);
            return;
        }

        // 예외 경로는 차단 여부와 무관하게 통과 (Spring Security가 2차 인증 담당)
        if (isExemptPath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (ipBlockStore.isBlocked(ip)) {
            log.warn("차단된 IP 접근 시도: ip={} uri={}", ip, uri);
            writeBlockedResponse(request, response);
            return;
        }

        filterChain.doFilter(request, response);

        if (response.getStatus() == HttpStatus.NOT_FOUND.value()
                && !Boolean.TRUE.equals(request.getAttribute(IpBlockAttributes.BUSINESS_NOT_FOUND))
                && !isNotFoundExemptPath(uri)) {
            ipBlockStore.incrementNotFoundAndBlockIfExceeded(ip);
        }
    }

    private void writeBlockedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        addCorsHeaders(request, response);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }

        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, GlobalErrorCode.IP_BLOCKED.getMessage());
        problemDetail.setProperty("errorCode", GlobalErrorCode.IP_BLOCKED.getCode());
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("exception", IpBlockFilter.class.getSimpleName());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problemDetail);
    }

    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        final String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods",
                    resolveOrDefault(request.getHeader("Access-Control-Request-Method"),
                            "GET, POST, PUT, PATCH, DELETE, OPTIONS"));
            response.setHeader("Access-Control-Allow-Headers",
                    resolveOrDefault(request.getHeader("Access-Control-Request-Headers"),
                            "Authorization, Content-Type, Accept, Origin, X-Requested-With"));
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.addHeader("Vary", "Origin");
            response.addHeader("Vary", "Access-Control-Request-Method");
            response.addHeader("Vary", "Access-Control-Request-Headers");
        }
    }

    private String resolveOrDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private boolean isExemptPath(String uri) {
        for (final String prefix : properties.exemptPaths()) {
            if (uri.equals(prefix) || uri.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 404 누적 카운트에서 제외할 경로인지 판단한다.
     * WebSocket(SockJS) 엔드포인트는 재연결·전송 폴백 과정에서 정상적으로 4xx를 유발하므로
     * 차단 카운트에 반영하면 정상 사용자가 차단될 수 있다.
     */
    private boolean isNotFoundExemptPath(String uri) {
        for (final String prefix : properties.notFoundExemptPaths()) {
            if (uri.equals(prefix) || uri.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 사설(RFC1918)·루프백·링크로컬·CGNAT(RFC6598)·IPv6 ULA(RFC4193) IP 여부.
     * isValidIp 통과 후 호출되므로 DNS 조회는 발생하지 않는다.
     */
    private boolean isInternalIp(String ip) {
        try {
            return isInternalAddress(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private boolean isInternalAddress(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isAnyLocalAddress()
                || isCarrierGradeNat(address)
                || isUniqueLocalIpv6(address);
    }

    /**
     * 100.64.0.0/10 (RFC 6598 CGNAT). 클라우드·k8s 프록시가 사용할 수 있으나
     * {@link InetAddress#isSiteLocalAddress()}는 포함하지 않으므로 직접 판정한다.
     */
    private boolean isCarrierGradeNat(InetAddress address) {
        final byte[] bytes = address.getAddress();
        if (bytes.length != 4) {
            return false;
        }
        final int first = bytes[0] & 0xFF;
        final int second = bytes[1] & 0xFF;
        return first == 100 && second >= 64 && second <= 127;
    }

    /**
     * fc00::/7 (RFC 4193 IPv6 Unique Local Address). 첫 바이트가 0xFC 또는 0xFD.
     */
    private boolean isUniqueLocalIpv6(InetAddress address) {
        final byte[] bytes = address.getAddress();
        if (bytes.length != 16) {
            return false;
        }
        return (bytes[0] & 0xFE) == 0xFC;
    }

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches();
    }
}
