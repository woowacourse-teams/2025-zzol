package coffeeshout.global.ipblock;

import coffeeshout.global.exception.GlobalErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
                && !Boolean.TRUE.equals(request.getAttribute(IpBlockAttributes.BUSINESS_NOT_FOUND))) {
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
