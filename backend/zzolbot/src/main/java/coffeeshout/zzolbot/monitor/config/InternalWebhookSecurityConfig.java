package coffeeshout.zzolbot.monitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * {@code /internal/**}(Alertmanager 웹훅) 전용 SecurityFilterChain(ADR-0032).
 *
 * <p>{@code @Order(0)}으로 admin(`/admin/**`, Order 1)·user(Order 2) 체인보다 먼저 매칭한다.
 * 베어러 토큰 검증({@link InternalWebhookTokenFilter})이 단일 게이트이므로 인가 규칙은 permitAll로 두고,
 * 토큰 불일치는 필터가 401로 차단한다. 머신 호출이라 CSRF는 끄고 세션은 stateless다.
 *
 * <p>토큰은 env {@code ZZOL_BOT_ALERT_WEBHOOK_TOKEN}로 주입한다(Alertmanager의 credentials_file과 동일 값).
 * 미설정이면 필터가 모든 요청을 거부한다(secure-by-default).
 */
@Configuration
public class InternalWebhookSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain internalWebhookFilterChain(
            HttpSecurity http,
            @Value("${ZZOL_BOT_ALERT_WEBHOOK_TOKEN:}") String webhookToken) throws Exception {
        http
                .securityMatcher("/internal/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        new InternalWebhookTokenFilter(webhookToken),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
