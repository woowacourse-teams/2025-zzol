package coffeeshout.room.infra;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import coffeeshout.room.config.PlayerNameAuditProperties;
import coffeeshout.room.domain.audit.PlayerNameAuditErrorCode;
import coffeeshout.room.domain.audit.PlayerNameAuditResult;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.infra.nickname.audit.GeminiPlayerNameAuditor;
import coffeeshout.room.infra.nickname.audit.PlayerNameAuditPromptTemplate;
import coffeeshout.room.infra.nickname.persistence.PlayerNameFeedbackJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GeminiPlayerNameAuditorParseTest {

    private static final PlayerNameAuditProperties PROPERTIES =
            new PlayerNameAuditProperties(null, "gemini-2.5-flash", 0.85, 10, 5);

    GeminiPlayerNameAuditor auditor;

    @BeforeEach
    void setUp() {
        final ObjectMapper objectMapper = new ObjectMapper();
        auditor = new GeminiPlayerNameAuditor(
                null,
                objectMapper,
                PROPERTIES,
                mock(PlayerNameFeedbackJpaRepository.class),
                new PlayerNameAuditPromptTemplate(objectMapper),
                new SimpleMeterRegistry()
        );
    }

    private List<PlayerNameAuditResult> parse(String responseText, List<String> playerNames) {
        return ReflectionTestUtils.invokeMethod(auditor, "parseResults", responseText, playerNames);
    }

    @Nested
    class JSON_전체_파싱_실패 {

        @Test
        void InfrastructureException을_던진다() {
            assertCoffeeShoutException(
                    () -> parse("invalid json", List.of("닉네임")),
                    PlayerNameAuditErrorCode.AI_RESPONSE_PARSE_FAILED
            );
        }

        @Test
        void 배치_파싱_실패_카운터가_증가한다() {
            final SimpleMeterRegistry registry = new SimpleMeterRegistry();
            final ObjectMapper objectMapper = new ObjectMapper();
            final GeminiPlayerNameAuditor localAuditor = new GeminiPlayerNameAuditor(
                    null, objectMapper, PROPERTIES,
                    mock(PlayerNameFeedbackJpaRepository.class),
                    new PlayerNameAuditPromptTemplate(objectMapper),
                    registry
            );

            try {
                ReflectionTestUtils.invokeMethod(localAuditor, "parseResults", "not json at all", List.of("닉네임"));
            } catch (Exception ignored) {
            }

            assertThat(registry.counter("playerName.audit.gemini.parse.failures").count()).isEqualTo(1);
        }
    }

    @Nested
    class 항목_파싱_실패 {

        @Test
        void 파싱_실패_항목은_PENDING으로_처리한다() {
            final String responseText = """
                    [
                      {"playerName": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "정상"},
                      {"playerName": "씨발", "flagged": true, "confidence": "숫자아님", "reason": "욕설"}
                    ]
                    """;

            final List<PlayerNameAuditResult> results = parse(responseText, List.of("용감한호랑이", "씨발"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(results).hasSize(2);
                softly.assertThat(results.get(0).status()).isEqualTo(PlayerNameAuditStatus.CLEAN);
                softly.assertThat(results.get(1).status()).isEqualTo(PlayerNameAuditStatus.PENDING);
            });
        }

        @Test
        void 항목_파싱_실패_카운터가_증가한다() {
            final String responseText = """
                    [{"playerName": "씨발", "flagged": true, "confidence": "숫자아님", "reason": "욕설"}]
                    """;

            final SimpleMeterRegistry registry = new SimpleMeterRegistry();
            final ObjectMapper objectMapper = new ObjectMapper();
            final GeminiPlayerNameAuditor localAuditor = new GeminiPlayerNameAuditor(
                    null, objectMapper, PROPERTIES,
                    mock(PlayerNameFeedbackJpaRepository.class),
                    new PlayerNameAuditPromptTemplate(objectMapper),
                    registry
            );
            ReflectionTestUtils.invokeMethod(localAuditor, "parseResults", responseText, List.of("씨발"));

            assertThat(registry.counter("playerName.audit.gemini.item.parse.failures").count()).isEqualTo(1);
        }

        @Test
        void 전체_항목_파싱_실패_시_모두_PENDING으로_처리한다() {
            final String responseText = """
                    [
                      {"playerName": "닉1", "flagged": true, "confidence": "bad", "reason": "욕설"},
                      {"playerName": "닉2", "flagged": false, "confidence": "bad", "reason": "정상"}
                    ]
                    """;

            final List<PlayerNameAuditResult> results = parse(responseText, List.of("닉1", "닉2"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(results).hasSize(2);
                softly.assertThat(results).extracting(PlayerNameAuditResult::playerName)
                        .containsExactlyInAnyOrder("닉1", "닉2");
                softly.assertThat(results).allSatisfy(r ->
                        softly.assertThat(r.status()).isEqualTo(PlayerNameAuditStatus.PENDING));
            });
        }
    }

    @Nested
    class 정상_파싱 {

        @Test
        void flagged_true이고_confidence가_threshold_이상이면_FLAGGED() {
            final String responseText = """
                    [{"playerName": "씨발", "flagged": true, "confidence": 0.9, "reason": "욕설"}]
                    """;

            final List<PlayerNameAuditResult> results = parse(responseText, List.of("씨발"));

            assertThat(results.getFirst().status()).isEqualTo(PlayerNameAuditStatus.FLAGGED);
        }

        @Test
        void flagged_true이고_confidence가_threshold_미만이면_PENDING() {
            final String responseText = """
                    [{"playerName": "씨발", "flagged": true, "confidence": 0.5, "reason": "애매함"}]
                    """;

            final List<PlayerNameAuditResult> results = parse(responseText, List.of("씨발"));

            assertThat(results.getFirst().status()).isEqualTo(PlayerNameAuditStatus.PENDING);
        }

        @Test
        void flagged_false이면_CLEAN() {
            final String responseText = """
                    [{"playerName": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "정상"}]
                    """;

            final List<PlayerNameAuditResult> results = parse(responseText, List.of("용감한호랑이"));

            assertThat(results.getFirst().status()).isEqualTo(PlayerNameAuditStatus.CLEAN);
        }

        @Test
        void 여러_항목을_모두_파싱한다() {
            final String responseText = """
                    [
                      {"playerName": "씨발", "flagged": true, "confidence": 0.97, "reason": "욕설"},
                      {"playerName": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "정상"}
                    ]
                    """;

            final List<PlayerNameAuditResult> results = parse(responseText, List.of("씨발", "용감한호랑이"));

            assertThat(results).hasSize(2);
        }
    }
}
