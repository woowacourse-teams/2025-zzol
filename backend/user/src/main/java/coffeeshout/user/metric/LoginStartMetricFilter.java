package coffeeshout.user.metric;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code /oauth2/authorization/{provider}} 진입을 세어 로그인 "시도"(분모)를 만든다.
 *
 * <p>인가 리다이렉트 필터(OAuth2AuthorizationRequestRedirectFilter)가 응답을 종료하기
 * 전에 실행돼야 카운트가 남으므로, 시큐리티 체인에서 그 필터 앞에 등록한다.
 */
public class LoginStartMetricFilter extends OncePerRequestFilter {

    private static final String PREFIX = "/oauth2/authorization/";

    private final LoginMetrics loginMetrics;

    public LoginStartMetricFilter(LoginMetrics loginMetrics) {
        this.loginMetrics = loginMetrics;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String uri = request.getRequestURI();
        if (uri != null && uri.startsWith(PREFIX)) {
            final String provider = uri.substring(PREFIX.length());
            // 하위 경로 없이 provider 세그먼트 하나일 때만 센다 (예: /oauth2/authorization/kakao)
            if (!provider.isEmpty() && provider.indexOf('/') < 0) {
                loginMetrics.countStart(provider);
            }
        }
        filterChain.doFilter(request, response);
    }
}
