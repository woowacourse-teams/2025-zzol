package coffeeshout.admin.auth;

import coffeeshout.admin.auth.domain.AdminPrincipal;
import coffeeshout.admin.auth.domain.AdminTokenIssuer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer} 헤더의 관리자 토큰을 파싱해 인증 컨텍스트를 채운다.
 *
 * <p>토큰이 없거나 유효하지 않으면 인증을 채우지 않고 그냥 통과시킨다. 거부는
 * {@code authorizeHttpRequests}가 한다. 필터가 직접 응답을 쓰면 공개 경로(로그인)까지 막힌다.
 */
@Slf4j
@RequiredArgsConstructor
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final AdminTokenIssuer adminTokenIssuer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = header.substring(BEARER_PREFIX.length());
        try {
            final AdminPrincipal principal = adminTokenIssuer.verify(token);
            final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority(ROLE_ADMIN)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.debug("관리자 토큰 인증 실패, 익명 처리: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
