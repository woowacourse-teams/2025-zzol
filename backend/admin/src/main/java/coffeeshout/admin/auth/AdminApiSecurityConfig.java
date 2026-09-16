package coffeeshout.admin.auth;

import coffeeshout.admin.auth.domain.AdminTokenIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 신규 백오피스 SPA(admin-web)가 쓰는 REST 체인.
 *
 * <p>한때 Thymeleaf 백오피스 체인이 {@code @Order(2)}로 함께 있었다. 신규 SPA 로 넘어오면서
 * 걷어냈고, 이제 관리자 경로를 지키는 체인은 이것 하나다.
 *
 * <p>필터 체인 순서: 0 = ws-catalog·internal webhook, <b>1 = admin API</b>,
 * 3 = user(매처 없는 fallback). 2번은 비어 있다 - 번호를 당기지 않는 이유는,
 * 순서 값이 곧 이 파일들 사이의 약속이라 다시 매기면 전부 고쳐야 하기 때문이다.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminAuthProperties.class)
public class AdminApiSecurityConfig {

    private static final String LOGIN_PATH = "/admin/api/auth/login";

    private final AdminTokenIssuer adminTokenIssuer;

    @Bean
    @Order(1)
    public SecurityFilterChain adminApiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/admin/api/**")
                // 없으면 admin-web(다른 오리진)의 프리플라이트가 인증 단계에서 막힌다(postmortem 0003).
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.requestMatchers(publicPaths())
                        .permitAll()
                        .anyRequest()
                        .hasRole("ADMIN"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // formLogin·httpBasic 을 끄면 인증 진입점이 사라져 스프링이 기본값인
                // Http403ForbiddenEntryPoint 를 쓴다. 그러면 미인증도 403 이 되어
                // SPA 가 "토큰 재발급하면 되는 상황"과 "권한이 없어 소용없는 상황"을 구분하지 못한다.
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // CSRF 를 끈다. CodeQL 이 java/spring-disabled-csrf-protection 으로 잡지만 오탐이다.
                //
                // CSRF 는 브라우저가 자격증명을 <b>자동으로</b> 실어 보낼 때 성립한다. 이 체인은
                // 세션이 없고(STATELESS), 토큰을 Authorization 헤더로만 읽으며, SPA 는 그 토큰을
                // 쿠키가 아니라 localStorage 에 둔다. 다른 오리진의 스크립트는 그 값을 읽을 수 없고
                // 헤더를 대신 붙여 줄 수도 없다. 실어 보낼 것이 없으니 위조할 요청도 없다.
                //
                // 반대로 켜면 SPA 가 매 요청마다 토큰을 받아 되돌려주는 왕복을 해야 하는데,
                // 얻는 것이 없다. 쿠키 인증으로 바꾸는 날에는 이 줄부터 되돌려야 한다.
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(
                        new AdminJwtAuthenticationFilter(adminTokenIssuer), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 인증 없이 열어 두는 경로. <b>로그인 하나뿐이다.</b>
     *
     * <p>한때 {@code local} 프로필에서만 열리는 dev-login 이 여기 있었다. 구글 검증을
     * 건너뛰고 허용목록만 보는 경로였는데 걷어냈다. 프로필로 잠그는 것은 프로필을
     * 잘못 띄우는 순간 무력해진다 - 경로 자체가 없으면 그 실수가 성립하지 않는다.
     *
     * <p>로컬에서도 실제 구글 로그인을 쓴다. 승인된 자바스크립트 원본에
     * {@code http://localhost:5173} 이 들어 있어 그대로 된다.
     */
    private String[] publicPaths() {
        return new String[] {LOGIN_PATH};
    }
}
