package coffeeshout.user.auth;

import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.infra.oauth.CustomOAuth2UserService;
import coffeeshout.user.metric.LoginMetrics;
import coffeeshout.user.metric.LoginStartMetricFilter;
import coffeeshout.user.ui.OAuthSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class UserSecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuthSuccessHandler oAuthSuccessHandler;
    private final AuthTokenService authTokenService;
    private final LoginMetrics loginMetrics;

    @Bean
    @Order(2)
    public SecurityFilterChain userFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuthSuccessHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(
                        new JwtAuthenticationFilter(authTokenService),
                        UsernamePasswordAuthenticationFilter.class
                )
                // 로그인 "시도" 카운트 — 인가 리다이렉트가 응답을 끝내기 전에 세야 한다
                .addFilterBefore(
                        new LoginStartMetricFilter(loginMetrics),
                        OAuth2AuthorizationRequestRedirectFilter.class
                );
        return http.build();
    }
}
