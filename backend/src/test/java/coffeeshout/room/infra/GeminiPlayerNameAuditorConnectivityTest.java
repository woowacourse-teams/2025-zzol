package coffeeshout.room.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import coffeeshout.room.config.PlayerNameAuditProperties;
import coffeeshout.room.domain.audit.PlayerNameAuditResult;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackJpaRepository;
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
 * 실행 방법: 1. 환경변수 GEMINI_API_KEY 설정 2. ./gradlew test --tests "*.GeminiPlayerNameAuditorConnectivityTest"
 * <p>
 * GEMINI_API_KEY 미설정 시 테스트가 자동으로 skip된다.
 */
@Disabled("실제 Gemini API 연결 테스트 - 수동 실행 전용")
class GeminiPlayerNameAuditorConnectivityTest {

    private GeminiPlayerNameAuditor auditor;


    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        assumeThat(apiKey)
                .as("GEMINI_API_KEY 환경변수가 설정되지 않아 테스트를 건너뜁니다.")
                .isNotNull()
                .isNotBlank();

        PlayerNameAuditProperties properties = new PlayerNameAuditProperties(
                apiKey,
                List.of("gemini-2.5-flash"),
                0.85,
                50,
                20
        );

        PlayerNameFeedbackJpaRepository feedbackRepository = mock(PlayerNameFeedbackJpaRepository.class);
        when(feedbackRepository.findRecentFeedbacks(any(Pageable.class))).thenReturn(List.of());

        auditor = new GeminiPlayerNameAuditor(
                Client.builder().apiKey(apiKey).build(),
                new ObjectMapper(),
                properties,
                feedbackRepository,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void 명확한_비속어는_FLAGGED로_판정한다() {
        List<PlayerNameAuditResult> results = auditor.audit(List.of("씨발놈", "용감한호랑이"));

        PlayerNameAuditResult profanity = findByNickname(results, "씨발놈");
        PlayerNameAuditResult normal = findByNickname(results, "용감한호랑이");

        assertThat(profanity.status()).isEqualTo(PlayerNameAuditStatus.FLAGGED);
        assertThat(normal.status()).isEqualTo(PlayerNameAuditStatus.CLEAN);
    }

    @Test
    void 특수문자_우회_비속어는_탐지한다() {
        List<PlayerNameAuditResult> results = auditor.audit(List.of("씨b알", "빠른여우"));

        PlayerNameAuditResult bypassed = findByNickname(results, "씨b알");
        assertThat(bypassed.status()).isIn(PlayerNameAuditStatus.FLAGGED, PlayerNameAuditStatus.PENDING);
    }

    @Test
    void 모든_닉네임에_대해_결과가_반환된다() {
        List<String> playerNames = List.of("용감한호랑이", "빠른여우", "작은곰");

        List<PlayerNameAuditResult> results = auditor.audit(playerNames);

        assertThat(results).hasSize(playerNames.size())
                .allSatisfy(result -> {
                    assertThat(result.playerName()).isNotBlank();
                    assertThat(result.status()).isNotNull();
                    assertThat(result.confidence().value().doubleValue()).isBetween(0.0, 1.0);
                });
    }

    private PlayerNameAuditResult findByNickname(List<PlayerNameAuditResult> results, String playerName) {
        return results.stream()
                .filter(r -> r.playerName().equals(playerName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("결과에서 닉네임을 찾을 수 없음: " + playerName));
    }
}
