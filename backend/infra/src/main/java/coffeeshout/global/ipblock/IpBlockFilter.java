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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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
 *   <li>매핑 없는 경로의 404 응답이 누적되면 차단 (IpBlockStore 위임). 컨트롤러가 낸 404는 세지 않는다(#1757)</li>
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

    private final IpBlockStore ipBlockStore;
    private final MaliciousPathMatcher maliciousPathMatcher;
    private final IpBlockProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    // PMD.NcssCount 억제 — 이 메서드는 조기 반환 가드가 순서대로 늘어선 형태이고,
    // 그 순서 자체가 보안 의미다(내부 IP → 악성 경로 → 예외 경로 → 차단 여부, postmortem 0003).
    // 절반을 헬퍼로 옮기면 순서가 한눈에 안 보여 오히려 위험해진다.
    @SuppressWarnings("PMD.NcssCount")
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        final String uri = request.getRequestURI();
        final Optional<Ip> clientIp = Ip.tryFrom(getClientIp(request));

        if (clientIp.isEmpty()) {
            // 검증 전 원본 값은 로그 인젝션 가능성이 있으므로 기록하지 않는다
            log.debug("유효하지 않은 IP 형식, 필터 처리 건너뜀: uri={}", uri);
            filterChain.doFilter(request, response);
            return;
        }
        final Ip ip = clientIp.get();

        // 사설/내부 IP(프록시·헬스체크·내부 서비스)는 클라이언트가 아니므로 차단·카운트 대상에서 제외한다.
        // XFF 신뢰 체인이 정상이면 여기 오는 값은 실제 클라이언트 IP이므로 내부 IP는 거의 나오지 않는다.
        // 체인이 끊기면(프록시가 XFF를 세팅하지 않으면) nginx의 도커 IP가 그대로 잡히고, 이를 차단하면
        // nginx 경유 트래픽 전체가 막혀 장애로 번진다(postmortem 0003). 내부 IP의 악성 경로 접근은
        // 프록시/XFF 설정 이상 신호이므로 차단 없이 경고만 남기고 통과시킨다.
        if (isInternalIp(ip.value())) {
            if (maliciousPathMatcher.isMalicious(uri)) {
                log.warn("내부 IP에서 악성 경로 접근 — 차단하지 않고 통과(프록시/XFF 설정 점검 필요): ip={} uri={}", ip, uri);
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
                && Boolean.TRUE.equals(request.getAttribute(IpBlockAttributes.UNMATCHED_NOT_FOUND))) {
            ipBlockStore.incrementNotFoundAndBlockIfExceeded(ip);
        }
    }

    private void writeBlockedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        addCorsHeaders(request, response);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }

        final ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, GlobalErrorCode.IP_BLOCKED.getMessage());
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
            response.setHeader(
                    "Access-Control-Allow-Methods",
                    resolveOrDefault(
                            request.getHeader("Access-Control-Request-Method"),
                            "GET, POST, PUT, PATCH, DELETE, OPTIONS"));
            response.setHeader(
                    "Access-Control-Allow-Headers",
                    resolveOrDefault(
                            request.getHeader("Access-Control-Request-Headers"),
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
     * 클라이언트 IP. Tomcat {@code RemoteIpValve}({@code server.forward-headers-strategy: native})가
     * 이미 X-Forwarded-For를 해석해 넣어둔 값이므로 여기서 헤더를 직접 읽지 않는다.
     *
     * <p>Valve는 XFF를 <b>오른쪽부터</b> 훑으며 {@code internal-proxies}에 매칭되는 항목만 벗겨내고
     * 첫 비매칭 IP에서 멈춘다. dev·prod는 Tomcat 기본값을 덮어써 {@code 127.0.0.1}과 RFC1918
     * 세 대역만 신뢰한다(application-{dev,prod}.yml). nginx가 {@code $proxy_add_x_forwarded_for}로
     * 실제 TCP peer를 XFF 맨 뒤에 덧붙이므로(proxy-http.inc·proxy-ws.inc), <b>공인 IP에서 온 요청이라면</b>
     * 클라이언트가 헤더 왼쪽에 주입한 사설 IP는 이 값이 될 수 없다 — XFF 스푸핑으로 아래 내부 IP
     * 화이트리스트를 통과할 수 없다. TCP peer 자체가 사설 IP인 요청은 주입값이 남을 수 있으나,
     * 그런 출발지는 스푸핑 없이도 이미 화이트리스트 대상이다.
     *
     * <p>이 보장은 두 전제에 의존한다.
     * <ol>
     *   <li>모든 {@code proxy_pass}가 XFF 체인을 누적한다 —
     *       {@code .github/scripts/check-nginx-proxy-headers.py}가 CI에서 강제한다(#1620).</li>
     *   <li>앱 포트가 nginx를 통해서만 도달 가능하다 — 앱 컨테이너는 host 포트를 열지 않고
     *       nginx만 80·443을 연다(docker/{dev,prod}/docker-compose.yml). 강제 장치는 없다.</li>
     * </ol>
     */
    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /**
     * 사설(RFC1918)·루프백·링크로컬·CGNAT(RFC6598)·IPv6 ULA(RFC4193) IP 여부.
     * {@link Ip} 검증을 통과한 IP 리터럴만 전달되므로 {@link InetAddress#getByName}은 DNS 조회를 하지 않는다.
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
}
