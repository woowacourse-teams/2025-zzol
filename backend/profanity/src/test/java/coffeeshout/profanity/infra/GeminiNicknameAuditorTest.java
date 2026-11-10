package coffeeshout.profanity.infra;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.domain.audit.NicknameAuditErrorCode;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.fixture.NicknameAuditPropertiesFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * 하루 호출 상한이 실제 HTTP 요청을 막는지 검증한다(#1907).
 *
 * <p>Gemini 자리에 로컬 HTTP 서버를 세우고 도착한 요청 수를 센다. 상한이 재려는 것이 그 숫자라서다.
 */
class GeminiNicknameAuditorTest {

    private static final String 정상_응답 = """
            {"candidates":[{"content":{"role":"model","parts":[{"text":\
            "[{\\"nickname\\":\\"용감한호랑이\\",\\"flagged\\":false,\\"confidence\\":0.1,\\"reason\\":\\"정상\\",\\"terms\\":[]}]"\
            }]},"finishReason":"STOP"}]}
            """;

    private HttpServer gemini;
    private AtomicInteger receivedRequests;
    private AtomicInteger remainingBudget;
    private SimpleMeterRegistry meterRegistry;
    private GeminiNicknameAuditor auditor;

    @BeforeEach
    void setUp() throws IOException {
        receivedRequests = new AtomicInteger();
        gemini = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        gemini.createContext("/", exchange -> {
            receivedRequests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            final byte[] body = 정상_응답.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        gemini.start();

        final Client client = Client.builder()
                .apiKey("test-key")
                .httpOptions(HttpOptions.builder()
                        .baseUrl("http://127.0.0.1:" + gemini.getAddress().getPort())
                        .build())
                .build();
        final ObjectMapper objectMapper = new ObjectMapper();
        final NicknameFeedbackRepository feedbackRepository = mock(NicknameFeedbackRepository.class);
        given(feedbackRepository.findRecentFeedbacks(any(PageRequest.class))).willReturn(List.of());

        remainingBudget = new AtomicInteger();
        meterRegistry = new SimpleMeterRegistry();
        auditor = new GeminiNicknameAuditor(
                client,
                objectMapper,
                NicknameAuditPropertiesFixture.API_키("test-key"),
                feedbackRepository,
                new NicknameAuditPromptTemplate(objectMapper),
                () -> remainingBudget.getAndDecrement() > 0,
                meterRegistry);
    }

    @AfterEach
    void tearDown() {
        gemini.stop(0);
    }

    @Nested
    class 하루_호출_상한 {

        @Test
        void 예산이_남아_있으면_Gemini를_부르고_판정을_돌려준다() {
            remainingBudget.set(1);

            final List<NicknameAuditResult> results = auditor.audit(List.of("용감한호랑이"));

            assertThat(results).extracting(NicknameAuditResult::status).containsExactly(NicknameAuditStatus.CLEAN);
            assertThat(receivedRequests).hasValue(1);
        }

        @Test
        void 상한에_닿으면_Gemini에_요청을_보내지_않는다() {
            remainingBudget.set(0);

            assertCoffeeShoutException(
                    () -> auditor.audit(List.of("용감한호랑이")), NicknameAuditErrorCode.AI_DAILY_CALL_LIMIT_REACHED);

            assertThat(receivedRequests)
                    .as("상한을 넘긴 요청이 Gemini까지 나가면 무료 티어 하루 한도를 태운다.")
                    .hasValue(0);
        }

        @Test
        void 호출마다_예산을_한_번씩_쓴다() {
            remainingBudget.set(2);

            auditor.audit(List.of("용감한호랑이"));
            auditor.audit(List.of("용감한호랑이"));
            assertCoffeeShoutException(
                    () -> auditor.audit(List.of("용감한호랑이")), NicknameAuditErrorCode.AI_DAILY_CALL_LIMIT_REACHED);

            assertThat(receivedRequests).hasValue(2);
        }

        @Test
        void 상한에_막힌_호출_수를_메트릭으로_남긴다() {
            remainingBudget.set(0);

            assertCoffeeShoutException(
                    () -> auditor.audit(List.of("용감한호랑이")), NicknameAuditErrorCode.AI_DAILY_CALL_LIMIT_REACHED);

            assertThat(meterRegistry
                            .get("nickname.audit.gemini.daily.limit.rejected")
                            .counter()
                            .count())
                    .isEqualTo(1.0);
        }
    }
}
