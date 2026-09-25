package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import coffeeshout.profanity.eval.NicknameAuditEvaluation;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.Predicted;
import coffeeshout.profanity.eval.NicknameAuditEvaluator;
import coffeeshout.profanity.fixture.NicknameAuditPropertiesFixture;
import coffeeshout.profanity.fixture.NicknameGoldenSetFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 실제 Gemini로 골든셋을 돌려 리포트를 쓴다. 기본 test 태스크에서 빠지고 {@code :profanity:goldenTest}로만 돈다.
 *
 * <p>스프링 밖이라 운영의 {@code @Retry}·{@code @RateLimiter}가 붙지 않는다. 같은 설정을 코드로 감싼다.
 */
@Slf4j
@Tag("golden")
class NicknameAuditGoldenTest {

    private static final int 배치_크기 = 100;
    private static final int 반복 = 3;
    // 무료 등급 RPM 15보다 넉넉하게 호출 사이를 5초 띄운다
    private static final Duration 호출_간격 = Duration.ofSeconds(5);

    @Test
    void 골든셋으로_현재_프롬프트를_평가해_리포트를_쓴다() throws IOException {
        final String apiKey = System.getenv("GEMINI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY가 없어 건너뛴다");
        final String model = Objects.requireNonNullElse(System.getenv("NICKNAME_AUDIT_MODEL"), "gemini-3.5-flash");
        final NicknameAuditProperties properties = NicknameAuditPropertiesFixture.모델(apiKey, model);

        final NicknameAuditEvaluation evaluation = new NicknameAuditEvaluator(
                        new TextNormalizer(), properties.minTermLength())
                .evaluate(NicknameGoldenSetFixture.골든셋(), 재시도와_간격을_둔(검열기(properties)), 배치_크기, 반복);

        final Path report = Path.of(
                "build/reports/golden",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".md");
        Files.createDirectories(report.getParent());
        Files.writeString(report, evaluation.toMarkdown(model + ", 피드백 예시 없는 기본 프롬프트"));
        log.info("골든셋 리포트: {}", report.toAbsolutePath());
        // 리포트를 먼저 써 두고 판정한다. 실패한 회차도 무엇이 비었는지 리포트로 남는다
        final int missing = evaluation.confusion().values().stream()
                .mapToInt(row -> row.get(Predicted.MISSING))
                .sum();
        assertThat(missing).as("모든 배치가 실패했다. 키·모델 이름·쿼터를 확인한다").isLessThan(evaluation.itemCount());
    }

    private static GeminiNicknameAuditor 검열기(NicknameAuditProperties properties) {
        final Client client = Client.builder()
                .apiKey(properties.geminiApiKey())
                .httpOptions(HttpOptions.builder()
                        .timeout(Math.toIntExact(properties.requestTimeout().toMillis()))
                        .build())
                .build();
        final ObjectMapper objectMapper = new ObjectMapper();
        // 목 저장소는 빈 목록을 돌려준다. 피드백 예시가 붙지 않아 기본 프롬프트만 잰다
        return new GeminiNicknameAuditor(
                client,
                objectMapper,
                properties,
                mock(NicknameFeedbackRepository.class),
                new NicknameAuditPromptTemplate(objectMapper),
                new SimpleMeterRegistry());
    }

    /** 운영 geminiAudit 재시도 설정(4회, 2초부터 두 배)을 따른다. 재시도도 호출 간격을 지킨다. */
    private static NicknameAuditor 재시도와_간격을_둔(NicknameAuditor auditor) {
        final RateLimiter rateLimiter = RateLimiter.of(
                "golden",
                RateLimiterConfig.custom()
                        .limitForPeriod(1)
                        .limitRefreshPeriod(호출_간격)
                        .timeoutDuration(Duration.ofMinutes(1))
                        .build());
        final Retry retry = Retry.of(
                "golden",
                RetryConfig.custom()
                        .maxAttempts(4)
                        .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofSeconds(2), 2))
                        .build());
        return nicknames -> Retry.decorateSupplier(
                        retry, RateLimiter.decorateSupplier(rateLimiter, () -> auditor.audit(nicknames)))
                .get();
    }
}
