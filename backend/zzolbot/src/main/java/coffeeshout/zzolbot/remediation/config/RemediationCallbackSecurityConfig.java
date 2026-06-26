package coffeeshout.zzolbot.remediation.config;

import coffeeshout.zzolbot.monitor.config.InternalWebhookTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * {@code /webhook/zzolbot/remediation/**}(GitHub Actions 워커 콜백) 전용 SecurityFilterChain.
 *
 * <p>Alertmanager 웹훅({@code /internal/**})과 달리 이 콜백은 GitHub 호스티드 러너(외부 인터넷)에서
 * 오므로 네트워크 격리가 불가능하다. 따라서 베어러 토큰이 단일 게이트다(GitHub 웹훅과 동일한 공개+시크릿 모델).
 * 위조 콜백의 피해 범위는 작다 — 이 경로는 수정 시도의 상태/PR 링크만 갱신하며 코드 실행·데이터 노출이 없다.
 *
 * <p>토큰은 alert 웹훅과 분리된 env {@code ZZOL_BOT_REMEDIATION_CALLBACK_TOKEN}로 주입한다.
 * 미설정이면 필터가 모든 요청을 거부한다(secure-by-default). {@code securityMatcher}가 서로 겹치지 않아
 * {@code @Order(0)}을 다른 전용 체인(/internal, /dev)과 함께 쓸 수 있다.
 */
@Configuration
public class RemediationCallbackSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain remediationCallbackFilterChain(
            HttpSecurity http,
            @Value("${ZZOL_BOT_REMEDIATION_CALLBACK_TOKEN:}") String callbackToken) throws Exception {
        http
                .securityMatcher("/webhook/zzolbot/remediation/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        new InternalWebhookTokenFilter(callbackToken),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
