package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

@Disabled("폴백 모델 목록(gemini-3-flash, gemini-3.5-flash, gemini-2.5-flash) 연결·200 응답 확인용 수동 테스트 — 실제 GEMINI_API_KEY 필요")
class GeminiNicknameAuditorConnectivityTest {

    private static final String API_KEY = System.getenv("GEMINI_API_KEY");
    private static final NicknameAuditProperties PROPERTIES = new NicknameAuditProperties(
            API_KEY,
            List.of("gemini-3-flash", "gemini-3.5-flash", "gemini-2.5-flash"),
            0.85,
            100,
            20
    );

    private GeminiNicknameAuditor auditor;

    @BeforeEach
    void setUp() {
        final Client client = Client.builder().apiKey(API_KEY).build();
        final ObjectMapper objectMapper = new ObjectMapper();
        final NicknameFeedbackRepository feedbackRepository = mock(NicknameFeedbackRepository.class);
        given(feedbackRepository.findRecentFeedbacks(any(PageRequest.class))).willReturn(List.of());

        auditor = new GeminiNicknameAuditor(
                client,
                objectMapper,
                PROPERTIES,
                feedbackRepository,
                new NicknameAuditPromptTemplate(objectMapper),
                new SimpleMeterRegistry()
        );
    }

    @Test
    void 정상_닉네임은_CLEAN으로_판정된다() {
        final List<NicknameAuditResult> results = auditor.audit(List.of("용감한호랑이"));

        assertSoftly(softly -> {
            softly.assertThat(results).hasSize(1);
            softly.assertThat(results.getFirst().status()).isEqualTo(NicknameAuditStatus.CLEAN);
        });
    }

    @Test
    void 명백한_비속어는_FLAGGED로_판정된다() {
        final List<NicknameAuditResult> results = auditor.audit(List.of("씨발"));

        assertSoftly(softly -> {
            softly.assertThat(results).hasSize(1);
            softly.assertThat(results.getFirst().status())
                    .isIn(NicknameAuditStatus.FLAGGED, NicknameAuditStatus.PENDING);
        });
    }

    @Test
    void 혼합_목록을_처리하고_입력_수만큼_결과를_반환한다() {
        final List<String> nicknames = List.of("용감한호랑이", "씨발", "1234닉네임");

        final List<NicknameAuditResult> results = auditor.audit(nicknames);

        assertThat(results).hasSize(nicknames.size());
    }
}
