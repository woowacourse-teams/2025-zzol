package coffeeshout.profanity.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.fixture.NicknameAuditPropertiesFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 응답을 받고도 요청한 닉네임에 판정을 붙이지 못한 항목은 결과에서 빠져야 한다.
 *
 * <p>PENDING으로 채우면 모델이 사람에게 넘긴 판정과 구분되지 않는다. 빠진 닉네임은 호출자가 시도 횟수를
 * 올려 다시 판정받는다. 네트워크 없이 응답 해석만 본다.
 */
class GeminiNicknameAuditorParseTest {

    private SimpleMeterRegistry meterRegistry;
    private GeminiNicknameAuditor auditor;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        auditor = new GeminiNicknameAuditor(
                null, new ObjectMapper(), NicknameAuditPropertiesFixture.API_키("api-key"), null, null, meterRegistry);
    }

    @Test
    void 형식이_어긋난_항목은_결과에서_빠지고_나머지는_남는다() {
        final String response = """
                [
                  {"nickname": "멀쩡닉", "flagged": false, "confidence": 0.99, "reason": "일반", "terms": []},
                  {"nickname": "깨진닉", "flagged": false, "confidence": "높음", "reason": "일반", "terms": []}
                ]
                """;

        final List<NicknameAuditResult> results = auditor.parseResults(response, List.of("멀쩡닉", "깨진닉"));

        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(results).extracting(NicknameAuditResult::nickname).containsExactly("멀쩡닉");
        softly.assertThat(meterRegistry
                        .counter("nickname.audit.gemini.item.parse.failures")
                        .count())
                .isEqualTo(1.0);
        softly.assertAll();
    }

    @Test
    void 응답_항목이_요청보다_적으면_빠진_닉네임은_결과에_없다() {
        final String response = """
                [{"nickname": "첫닉", "flagged": false, "confidence": 0.99, "reason": "일반", "terms": []}]
                """;

        final List<NicknameAuditResult> results = auditor.parseResults(response, List.of("첫닉", "둘닉"));

        assertThat(results).extracting(NicknameAuditResult::nickname).containsExactly("첫닉");
    }

    @Test
    void 모델이_닉네임을_바꿔_돌려주면_원래_닉네임의_결과를_지어내지_않는다() {
        final String response = """
                [{"nickname": "씨발", "flagged": true, "confidence": 0.95, "reason": "욕설", "terms": ["씨발"]}]
                """;

        final List<NicknameAuditResult> results = auditor.parseResults(response, List.of("씨b알"));

        assertThat(results).extracting(NicknameAuditResult::nickname).doesNotContain("씨b알");
    }
}
