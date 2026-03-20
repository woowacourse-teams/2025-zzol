package coffeeshout.room.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.room.config.NicknameAuditProperties;
import coffeeshout.room.domain.audit.NicknameAuditResult;
import coffeeshout.room.domain.audit.NicknameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.NicknameFeedbackJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GeminiNicknameAuditorParseTest {

    private static final NicknameAuditProperties PROPERTIES =
            new NicknameAuditProperties(null, "gemini-2.5-flash", 0.85, 10, 5);

    @Mock NicknameFeedbackJpaRepository feedbackRepository;

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    GeminiNicknameAuditor auditor;

    @BeforeEach
    void setUp() {
        auditor = new GeminiNicknameAuditor(null, new ObjectMapper(), PROPERTIES, feedbackRepository, meterRegistry);
        auditor.initMetrics();
    }

    private List<NicknameAuditResult> parse(String responseText, List<String> nicknames) {
        return ReflectionTestUtils.invokeMethod(auditor, "parseResults", responseText, nicknames);
    }

    @Nested
    class JSON_전체_파싱_실패 {

        @Test
        void 빈_리스트를_반환한다() {
            List<NicknameAuditResult> results = parse("invalid json", List.of("닉네임"));

            assertThat(results).isEmpty();
        }

        @Test
        void 배치_파싱_실패_카운터가_증가한다() {
            parse("not json at all", List.of("닉네임"));

            assertThat(meterRegistry.counter("nickname.audit.gemini.parse.failures").count()).isEqualTo(1);
        }
    }

    @Nested
    class 항목_파싱_실패 {

        @Test
        void 실패_항목을_제외한_나머지를_반환한다() {
            String responseText = """
                    [
                      {"nickname": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "정상"},
                      {"nickname": "씨발", "flagged": true, "confidence": "숫자아님", "reason": "욕설"}
                    ]
                    """;

            List<NicknameAuditResult> results = parse(responseText, List.of("용감한호랑이", "씨발"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(results).hasSize(1);
                softly.assertThat(results.get(0).nickname()).isEqualTo("용감한호랑이");
            });
        }

        @Test
        void 항목_파싱_실패_카운터가_증가한다() {
            String responseText = """
                    [{"nickname": "씨발", "flagged": true, "confidence": "숫자아님", "reason": "욕설"}]
                    """;

            parse(responseText, List.of("씨발"));

            assertThat(meterRegistry.counter("nickname.audit.gemini.item.parse.failures").count()).isEqualTo(1);
        }

        @Test
        void 전체_항목_실패_시_빈_리스트를_반환한다() {
            String responseText = """
                    [
                      {"nickname": "닉1", "flagged": true, "confidence": "bad", "reason": "욕설"},
                      {"nickname": "닉2", "flagged": false, "confidence": "bad", "reason": "정상"}
                    ]
                    """;

            List<NicknameAuditResult> results = parse(responseText, List.of("닉1", "닉2"));

            assertThat(results).isEmpty();
        }
    }

    @Nested
    class 정상_파싱 {

        @Test
        void flagged_true이고_confidence가_threshold_이상이면_FLAGGED() {
            String responseText = """
                    [{"nickname": "씨발", "flagged": true, "confidence": 0.9, "reason": "욕설"}]
                    """;

            List<NicknameAuditResult> results = parse(responseText, List.of("씨발"));

            assertThat(results.get(0).status()).isEqualTo(NicknameAuditStatus.FLAGGED);
        }

        @Test
        void flagged_true이고_confidence가_threshold_미만이면_PENDING() {
            String responseText = """
                    [{"nickname": "씨발", "flagged": true, "confidence": 0.5, "reason": "애매함"}]
                    """;

            List<NicknameAuditResult> results = parse(responseText, List.of("씨발"));

            assertThat(results.get(0).status()).isEqualTo(NicknameAuditStatus.PENDING);
        }

        @Test
        void flagged_false이면_CLEAN() {
            String responseText = """
                    [{"nickname": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "정상"}]
                    """;

            List<NicknameAuditResult> results = parse(responseText, List.of("용감한호랑이"));

            assertThat(results.get(0).status()).isEqualTo(NicknameAuditStatus.CLEAN);
        }

        @Test
        void 여러_항목을_모두_파싱한다() {
            String responseText = """
                    [
                      {"nickname": "씨발", "flagged": true, "confidence": 0.97, "reason": "욕설"},
                      {"nickname": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "정상"}
                    ]
                    """;

            List<NicknameAuditResult> results = parse(responseText, List.of("씨발", "용감한호랑이"));

            assertThat(results).hasSize(2);
        }
    }
}
