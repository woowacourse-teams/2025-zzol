package coffeeshout.user.infra.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OAuth 로그인 시도·성공 카운터.
 *
 * <p>이 지표들은 "성공이 사라지는" 유형의 조용한 장애를 잡기 위한 것이다. redirect_uri
 * 불일치(카카오 KOE006 등)는 백엔드가 5xx를 내지 않고 사용자가 provider 페이지에서
 * 튕겨 돌아오지 않으므로, 에러율로는 감지되지 않는다. 대신 {@code login.start}(시도)는
 * 계속 늘지만 {@code login.success}(성공)가 0으로 떨어지는 것으로 드러난다.
 * 알람은 이 둘의 비교로 발화한다(rules/alerts-app.yml).
 */
@Component
@RequiredArgsConstructor
public class LoginMetricService {

    private final MeterRegistry meterRegistry;

    /** OAuth 인가 시작(/oauth2/authorization/{provider}) 카운트 — 알람의 분모(시도). */
    public void countStart(String provider) {
        counter("login.start", "OAuth 인가 시작 요청 수", provider).increment();
    }

    /** 로그인 성공(토큰 발급 직전) 카운트 — 알람의 분자(성공). */
    public void countSuccess(String provider) {
        counter("login.success", "OAuth 로그인 성공 수", provider).increment();
    }

    private Counter counter(String name, String description, String provider) {
        return Counter.builder(name)
                .description(description)
                .tag("provider", provider)
                .register(meterRegistry);
    }
}
