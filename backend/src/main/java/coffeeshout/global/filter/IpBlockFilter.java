package coffeeshout.global.filter;

import coffeeshout.global.ratelimit.IpBlockStore;
import coffeeshout.global.ratelimit.MaliciousPathMatcher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IP 기반 차단 필터.
 * <p>
 * 세 가지 차단 정책을 적용한다:
 * <ol>
 *   <li>이미 차단된 IP는 요청 초입에서 즉시 429 반환</li>
 *   <li>악성 경로(스캐너 패턴) 접근 시 해당 IP를 즉시 차단하고 429 반환</li>
 *   <li>404 응답이 누적되면 차단 (IpBlockStore 위임)</li>
 * </ol>
 *
 * <p>{@code HIGHEST_PRECEDENCE}로 등록해 Security 필터 체인보다 먼저 실행한다.
 * 차단된 요청은 Security 처리 비용 없이 즉시 반환된다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class IpBlockFilter extends OncePerRequestFilter {

    private static final String BLOCKED_RESPONSE_BODY = """
            {"status":429,"detail":"비정상적인 접근으로 일시적으로 차단되었습니다.","errorCode":"IP_BLOCKED"}""";

    private final IpBlockStore ipBlockStore;
    private final MaliciousPathMatcher maliciousPathMatcher;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String ip = extractIp(request);

        if (ipBlockStore.isBlocked(ip)) {
            log.warn("차단된 IP 접근 시도: ip={} uri={}", ip, request.getRequestURI());
            writeBlockedResponse(response);
            return;
        }

        if (maliciousPathMatcher.isMalicious(request.getRequestURI())) {
            log.warn("악성 경로 접근 감지 → IP 즉시 차단: ip={} uri={}", ip, request.getRequestURI());
            ipBlockStore.blockImmediately(ip);
            writeBlockedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);

        if (response.getStatus() == HttpStatus.NOT_FOUND.value()) {
            ipBlockStore.incrementNotFoundAndBlockIfExceeded(ip);
        }
    }

    private String extractIp(HttpServletRequest request) {
        final String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeBlockedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(BLOCKED_RESPONSE_BODY);
    }
}
