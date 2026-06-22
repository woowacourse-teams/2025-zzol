package coffeeshout.zzolbot.monitor.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code /internal/**} 베어러 토큰 검증 필터(ADR-0032). Alertmanager가 보낸
 * {@code Authorization: Bearer <token>}를 설정된 토큰과 상수시간 비교한다.
 *
 * <p>네트워크 격리(nginx 내부 전용 리스너)가 1차 방어이고, 이 토큰은 2차 방어다 —
 * 모니터링 네트워크 내부의 침해된 컨테이너가 임의 알림을 주입하거나 LLM 예산을 소진하지 못하게 한다.
 *
 * <p>이 필터가 단일 게이트다 — 불일치면 401로 즉시 차단하고, 통과 시에만 체인을 진행한다.
 * 토큰이 비어 있으면(미설정) 모든 요청을 거부한다(secure-by-default).
 */
public class InternalWebhookTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final String expectedToken;

    public InternalWebhookTokenFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!matches(extractToken(request))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        final String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }

    private boolean matches(String provided) {
        if (expectedToken == null || expectedToken.isBlank() || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
