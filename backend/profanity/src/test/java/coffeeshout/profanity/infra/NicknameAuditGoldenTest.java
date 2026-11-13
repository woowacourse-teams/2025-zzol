package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import coffeeshout.profanity.eval.NicknameAuditEvaluation;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.TokenUsage;
import coffeeshout.profanity.eval.NicknameAuditEvaluator;
import coffeeshout.profanity.fixture.NicknameAuditPropertiesFixture;
import coffeeshout.profanity.fixture.NicknameGoldenSetFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 실제 Gemini로 골든셋을 돌려 리포트를 쓴다. 기본 test 태스크에서 빠지고 {@code :profanity:goldenTest}로만 돈다.
 *
 * <p>환경변수로 조건을 바꾼다. {@code NICKNAME_AUDIT_MODEL}, {@code NICKNAME_AUDIT_BATCH_SIZE}(기본 100),
 * {@code NICKNAME_AUDIT_REPEATS}(기본 3), {@code NICKNAME_AUDIT_TIMEOUT_SECONDS}(기본 120),
 * {@code NICKNAME_AUDIT_HTTP_ATTEMPTS}(기본은 SDK 기본 재시도. 1이면 재시도를 꺼서 배치 하나가 HTTP 요청 하나가 된다),
 * {@code NICKNAME_GOLDEN_SET}(기본 {@code golden}, 어려운 표본만 돌리려면 {@code hard}),
 * {@code NICKNAME_AUDIT_SHUFFLE_SEED}(기본 1907. 회차 n은 이 값에 n을 더한 시드로 닉네임 순서를 섞는다).
 *
 * <p>재시도는 SDK 기본 재시도(408·429·5xx, 5회)만 쓴다. 운영 {@code geminiAudit} 재시도는
 * {@code InfrastructureException}을 무시하는데 검열기가 모든 호출 오류를 그 예외로 감싸므로 운영에서도 돌지 않는다.
 * 여기에 재시도를 하나 더 얹으면 배치 하나가 HTTP 요청을 최대 20번 보내 무료 등급 일일 한도를 태운다.
 */
@Slf4j
@Tag("golden")
class NicknameAuditGoldenTest {

    // 무료 등급 RPM 15보다 넉넉하게 호출 사이를 5초 띄운다
    private static final Duration 호출_간격 = Duration.ofSeconds(5);

    @Test
    void 골든셋으로_현재_프롬프트를_평가해_리포트를_쓴다() throws IOException {
        final String apiKey = System.getenv("GEMINI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY가 없어 건너뛴다");
        final String model = Objects.requireNonNullElse(System.getenv("NICKNAME_AUDIT_MODEL"), "gemini-3.5-flash");
        final int batchSize = 정수_환경변수("NICKNAME_AUDIT_BATCH_SIZE", 100);
        final int repeats = 정수_환경변수("NICKNAME_AUDIT_REPEATS", 3);
        final Duration timeout = Duration.ofSeconds(정수_환경변수("NICKNAME_AUDIT_TIMEOUT_SECONDS", 120));
        final String goldenSet = NicknameGoldenSetFixture.세트_이름();
        final NicknameAuditProperties properties = NicknameAuditPropertiesFixture.모델(apiKey, model, timeout);
        final int httpAttempts = 정수_환경변수("NICKNAME_AUDIT_HTTP_ATTEMPTS", 0);
        final long shuffleSeed = 정수_환경변수("NICKNAME_AUDIT_SHUFFLE_SEED", 1907);
        final AtomicReference<TokenUsage> lastUsage = new AtomicReference<>(TokenUsage.NONE);

        final NicknameAuditEvaluation evaluation = new NicknameAuditEvaluator(
                        new TextNormalizer(), properties.minTermLength())
                .evaluate(
                        NicknameGoldenSetFixture.골든셋(goldenSet),
                        간격을_두고_한도_초과면_멈추는(검열기(properties, httpAttempts, lastUsage)),
                        batchSize,
                        repeats,
                        shuffleSeed,
                        () -> lastUsage.getAndSet(TokenUsage.NONE));

        final String reportName = "%s-%s-%s-b%d"
                .formatted(
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
                        goldenSet,
                        model,
                        batchSize);
        final Path report = Path.of("build/reports/golden", reportName + ".md");
        Files.createDirectories(report.getParent());
        Files.writeString(report, evaluation.toMarkdown(model + ", " + goldenSet + " 세트, 피드백 예시 없는 기본 프롬프트"));
        // 모델끼리 짝지어 비교하려면 닉네임별 판정이 필요하다(scripts/compare_models.py)
        Files.writeString(report.resolveSibling(reportName + ".tsv"), evaluation.toRowsTsv());
        Files.writeString(report.resolveSibling(reportName + ".calls.tsv"), evaluation.toCallsTsv());
        log.info("골든셋 리포트: {}", report.toAbsolutePath());
        // 리포트를 먼저 써 두고 판정한다. 실패한 회차도 무엇이 비었는지 리포트로 남는다
        assertThat(evaluation.missingByRun())
                .as("판정을 하나도 못 받은 회차가 있다. 키·모델 이름·쿼터를 확인한다")
                .allSatisfy(missing -> assertThat(missing).isLessThan(evaluation.itemCount()));
    }

    private static int 정수_환경변수(String name, int defaultValue) {
        final String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
    }

    private static GeminiNicknameAuditor 검열기(
            NicknameAuditProperties properties, int httpAttempts, AtomicReference<TokenUsage> lastUsage) {
        final HttpOptions.Builder httpOptions = HttpOptions.builder()
                .timeout(Math.toIntExact(properties.requestTimeout().toMillis()));
        if (httpAttempts > 0) {
            httpOptions.retryOptions(
                    HttpRetryOptions.builder().attempts(httpAttempts).build());
        }
        final Client client = Client.builder()
                .apiKey(properties.geminiApiKey())
                .httpOptions(httpOptions.build())
                .build();
        토큰_사용량을_엿본다(client, lastUsage);
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

    /**
     * 운영 검열기는 판정만 돌려주고 응답의 usage metadata는 버린다. 운영 코드를 건드리지 않으려고
     * SDK의 {@code models}를 스파이로 바꿔 끼워 실제 응답에서 토큰 수만 읽어 둔다.
     */
    private static void 토큰_사용량을_엿본다(Client client, AtomicReference<TokenUsage> lastUsage) {
        final Models spied = spy(client.models);
        doAnswer(invocation -> {
                    final GenerateContentResponse response = (GenerateContentResponse) invocation.callRealMethod();
                    response.usageMetadata()
                            .ifPresent(usage -> lastUsage.set(new TokenUsage(
                                    usage.promptTokenCount().orElse(0),
                                    usage.candidatesTokenCount().orElse(0),
                                    usage.cachedContentTokenCount().orElse(0),
                                    usage.thoughtsTokenCount().orElse(0),
                                    usage.totalTokenCount().orElse(0))));
                    return response;
                })
                .when(spied)
                .generateContent(anyString(), anyString(), any(GenerateContentConfig.class));
        ReflectionTestUtils.setField(client, "models", spied);
    }

    /**
     * 호출 사이 간격을 지키고, 429를 한 번 받으면 남은 배치는 호출하지 않고 실패시킨다.
     * 운영과 한도를 같이 쓰므로 한도에 닿은 뒤에는 더 부르지 않는다.
     */
    private static NicknameAuditor 간격을_두고_한도_초과면_멈추는(NicknameAuditor auditor) {
        final RateLimiter rateLimiter = RateLimiter.of(
                "golden",
                RateLimiterConfig.custom()
                        .limitForPeriod(1)
                        .limitRefreshPeriod(호출_간격)
                        .timeoutDuration(Duration.ofMinutes(1))
                        .build());
        final AtomicBoolean quotaExhausted = new AtomicBoolean();
        return nicknames -> {
            if (quotaExhausted.get()) {
                throw new IllegalStateException("429를 받아서 호출하지 않는다");
            }
            try {
                return RateLimiter.decorateSupplier(rateLimiter, () -> auditor.audit(nicknames))
                        .get();
            } catch (RuntimeException e) {
                if (일일_할당량_초과(e)) {
                    quotaExhausted.set(true);
                }
                throw e;
            }
        };
    }

    private static boolean 일일_할당량_초과(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ApiException api && api.code() == 429) {
                return true;
            }
        }
        return false;
    }
}
