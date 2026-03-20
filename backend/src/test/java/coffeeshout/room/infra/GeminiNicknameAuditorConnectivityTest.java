package coffeeshout.room.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import coffeeshout.room.config.NicknameAuditProperties;
import coffeeshout.room.domain.audit.NicknameAuditResult;
import coffeeshout.room.domain.audit.NicknameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.NicknameFeedbackJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * Gemini API 실제 연결 테스트. 수동 실행 전용 — CI에서는 제외된다.
 * <p>
 * 실행 방법: 1. 환경변수 GEMINI_API_KEY 설정 2. ./gradlew test --tests "*.GeminiNicknameAuditorConnectivityTest"
 * <p>
 * GEMINI_API_KEY 미설정 시 테스트가 자동으로 skip된다.
 */
@Disabled("실제 Gemini API 연결 테스트 - 수동 실행 전용")
class GeminiNicknameAuditorConnectivityTest {

    private GeminiNicknameAuditor auditor;


    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        assumeThat(apiKey)
                .as("GEMINI_API_KEY 환경변수가 설정되지 않아 테스트를 건너뜁니다.")
                .isNotNull()
                .isNotBlank();

        NicknameAuditProperties properties = new NicknameAuditProperties(
                apiKey,
                "gemini-2.5-flash",
                0.85,
                50,
                20,
                10
        );

        NicknameFeedbackJpaRepository feedbackRepository = mock(NicknameFeedbackJpaRepository.class);
        when(feedbackRepository.findRecentFeedbacks(any(Pageable.class))).thenReturn(List.of());

        auditor = new GeminiNicknameAuditor(
                Client.builder().apiKey(apiKey).build(),
                new ObjectMapper(),
                properties,
                feedbackRepository,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void 명확한_비속어는_FLAGGED로_판정한다() {
        List<NicknameAuditResult> results = auditor.audit(List.of("씨발놈", "용감한호랑이"));

        NicknameAuditResult profanity = findByNickname(results, "씨발놈");
        NicknameAuditResult normal = findByNickname(results, "용감한호랑이");

        assertThat(profanity.status()).isEqualTo(NicknameAuditStatus.FLAGGED);
        assertThat(normal.status()).isEqualTo(NicknameAuditStatus.CLEAN);
    }

    @Test
    void 특수문자_우회_비속어는_탐지한다() {
        List<NicknameAuditResult> results = auditor.audit(List.of("씨b알", "빠른여우"));

        NicknameAuditResult bypassed = findByNickname(results, "씨b알");
        assertThat(bypassed.status()).isIn(NicknameAuditStatus.FLAGGED, NicknameAuditStatus.PENDING);
    }

    @Test
    void 모든_닉네임에_대해_결과가_반환된다() {
        List<String> nicknames = List.of("용감한호랑이", "빠른여우", "작은곰");

        List<NicknameAuditResult> results = auditor.audit(nicknames);

        assertThat(results).hasSize(nicknames.size())
                .allSatisfy(result -> {
                    assertThat(result.nickname()).isNotBlank();
                    assertThat(result.status()).isNotNull();
                    assertThat(result.confidence()).isBetween(0.0, 1.0);
                });
    }

    private NicknameAuditResult findByNickname(List<NicknameAuditResult> results, String nickname) {
        return results.stream()
                .filter(r -> r.nickname().equals(nickname))
                .findFirst()
                .orElseThrow(() -> new AssertionError("결과에서 닉네임을 찾을 수 없음: " + nickname));
    }
}
