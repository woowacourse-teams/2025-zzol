package coffeeshout.user.infra.metric;

import coffeeshout.user.domain.OAuthProvider;
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

    private final LoginMetricService loginMetricService;

    public LoginStartMetricFilter(LoginMetricService loginMetricService) {
        this.loginMetricService = loginMetricService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String uri = request.getRequestURI();
        if (uri != null && uri.startsWith(PREFIX)) {
            // URL 세그먼트를 그대로 태그로 쓰면 임의 provider(예: /oauth2/authorization/<랜덤>)로
            // 메트릭 시리즈가 무한 생성돼 카디널리티가 폭발한다. 지원 provider일 때만, 그리고
            // enum의 정규화된 값(소문자)으로만 센다 — 성공 카운트 태그와도 값집합이 일치한다.
            final String segment = uri.substring(PREFIX.length());
            OAuthProvider.fromRegistrationId(segment)
                    .ifPresent(loginMetricService::countStart);
        }
        filterChain.doFilter(request, response);
    }
}
