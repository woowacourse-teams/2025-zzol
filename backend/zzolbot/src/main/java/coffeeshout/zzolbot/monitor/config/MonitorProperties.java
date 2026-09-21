package coffeeshout.zzolbot.monitor.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 모니터링 설정. 분석 활성 여부, ERROR 로그 조회 윈도우, 재분석 간격, 섀도우 분석을 외부화한다.
 */
@Validated
@ConfigurationProperties(prefix = "zzol-bot.monitor")
public record MonitorProperties(
        boolean enabled,
        @Positive int errorLogWindowMinutes,
        @PositiveOrZero int enrichCooldownMinutes,
        @Valid ShadowProperties shadow) {

    public MonitorProperties {
        // 설정에 shadow 블록이 없으면 꺼진 것으로 본다. 기본이 꺼짐이어야 기존 환경이 그대로 돈다
        if (shadow == null) {
            shadow = ShadowProperties.disabled();
        }
    }

    // 생성자를 하나 더 두면 안 된다. Spring Boot는 비-private 생성자가 정확히 하나일 때만
    // 바인딩 생성자를 추론하고, 둘이 되는 순간 JavaBean 바인딩으로 떨어져 record에 기본
    // 생성자가 없다는 이유로 컨텍스트가 뜨지 않는다. 테스트 편의 생성자를 넣었다가 실제로 겪었다.

    /**
     * ERROR 로그 샘플 조회 윈도우.
     */
    public Duration window() {
        return Duration.ofMinutes(errorLogWindowMinutes);
    }

    /**
     * 같은 fingerprint를 이 시간 안에는 다시 분석하지 않는다(지문별 중복 분석 방지). 웹훅 재시도·flapping을
     * 흡수하고, 지속되는 장애의 LLM 재호출을 fingerprint당 일정 시간에 한 번으로 묶어 비용을 통제한다. 0이면 비활성.
     */
    public Duration enrichCooldown() {
        return Duration.ofMinutes(enrichCooldownMinutes);
    }

    /**
     * 자체 호스팅 모델을 같은 입력으로 나란히 돌려 결과만 기록하는 섀도우 분석 설정.
     * 운영 판정은 바뀌지 않는다.
     *
     * @param enabled 꺼져 있으면 섀도우 빈이 아예 만들어지지 않는다
     * @param baseUrl llama.cpp 서버 주소
     * @param connectTimeoutMillis 연결 타임아웃
     * @param readTimeoutMillis 응답 타임아웃. CPU 추론이라 API보다 넉넉해야 한다.
     *                          최악을 실측값으로 계산하면 326초다(프롬프트 36.7 tok/s, 생성 8.4 tok/s)
     * @param maxTokens 생성 상한
     */
    public record ShadowProperties(
            boolean enabled,
            String baseUrl,
            @Positive int connectTimeoutMillis,
            @Positive int readTimeoutMillis,
            @Positive int maxTokens) {

        public static ShadowProperties disabled() {
            return new ShadowProperties(false, "http://127.0.0.1:8081", 2000, 420000, 1400);
        }

        public Duration connectTimeout() {
            return Duration.ofMillis(connectTimeoutMillis);
        }

        public Duration readTimeout() {
            return Duration.ofMillis(readTimeoutMillis);
        }
    }
}
