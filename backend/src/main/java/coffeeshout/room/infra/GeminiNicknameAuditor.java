package coffeeshout.room.infra;

import coffeeshout.room.config.NicknameAuditProperties;
import coffeeshout.room.domain.audit.NicknameAuditResult;
import coffeeshout.room.domain.audit.NicknameAuditor;
import coffeeshout.room.infra.persistence.NicknameFeedbackEntity;
import coffeeshout.room.infra.persistence.NicknameFeedbackJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class GeminiNicknameAuditor implements NicknameAuditor {

    private static final String BASE_SYSTEM_PROMPT = """
            너는 한국어 닉네임 검열 전문가다.
            아래 닉네임 목록을 검토하고 각 항목에 대해 JSON 배열로만 응답하라. 반드시 다른 텍스트는 포함하지 마라.
            
            비속어 판단 기준:
            - 직접적 욕설뿐 아니라 자모 분리, 특수문자 삽입, 유사 발음 대체로 우회한 경우 포함
            - 문화적 맥락을 고려한다 (예: "미쳤다"는 일반 감탄사로 사용되므로 flagged=false)
            - 판단이 애매한 경우 confidence를 낮게 설정한다
            
            응답 형식:
            [
              { "nickname": "씨b알",     "flagged": true,  "confidence": 0.97, "reason": "비속어 우회 (특수문자 삽입)" },
              { "nickname": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "일반 닉네임" }
            ]
            """;

    private final Client geminiClient;
    private final ObjectMapper objectMapper;
    private final NicknameAuditProperties properties;
    private final NicknameFeedbackJpaRepository feedbackRepository;
    private final MeterRegistry meterRegistry;

    private Timer apiCallTimer;
    private Counter parseFailureCounter;

    @PostConstruct
    void initMetrics() {
        apiCallTimer = Timer.builder("nickname.audit.gemini.call.duration")
                .description("Gemini API 호출 소요 시간")
                .register(meterRegistry);
        parseFailureCounter = Counter.builder("nickname.audit.gemini.parse.failures")
                .description("Gemini 응답 JSON 파싱 실패 횟수")
                .register(meterRegistry);
    }

    @Override
    @Retry(name = "geminiAudit")
    public List<NicknameAuditResult> audit(List<String> nicknames) {
        String prompt = buildPrompt(nicknames);

        try {
            GenerateContentResponse response = apiCallTimer.recordCallable(() ->
                    geminiClient.models.generateContent(
                            properties.model(),
                            prompt,
                            GenerateContentConfig.builder()
                                    .responseMimeType("application/json")
                                    .build()
                    ));

            return parseResults(response.text(), nicknames);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API 호출 실패", e);
        }
    }

    private String buildPrompt(List<String> nicknames) {
        StringBuilder prompt = new StringBuilder(BASE_SYSTEM_PROMPT);

        List<NicknameFeedbackEntity> feedbacks = feedbackRepository.findRecentFeedbacks(
                PageRequest.of(0, properties.feedbackInjectionThreshold(), Sort.by("createdAt").descending())
        );

        if (feedbacks.size() >= properties.feedbackInjectionThreshold()) {
            prompt.append("\n운영자 피드백 기반 추가 예시:\n");
            for (NicknameFeedbackEntity feedback : feedbacks) {
                boolean operatorFlagged =
                        feedback.getOperatorDecision() == NicknameFeedbackEntity.OperatorDecision.BLOCKED;
                double exampleConfidence = operatorFlagged ? 0.99 : 0.01;
                prompt.append(String.format(
                        "{ \"nickname\": \"%s\", \"flagged\": %b, \"confidence\": %.2f, \"reason\": \"운영자 피드백\" }%n",
                        feedback.getNickname(), operatorFlagged, exampleConfidence
                ));
            }
        }

        prompt.append("\n검열할 닉네임 목록:\n").append(nicknames);
        return prompt.toString();
    }

    private List<NicknameAuditResult> parseResults(String responseText, List<String> requestedNicknames) {
        try {
            List<GeminiAuditItem> items = objectMapper.readValue(responseText, new TypeReference<>() {
            });
            return items.stream()
                    .map(item -> NicknameAuditResult.of(
                            item.nickname(), item.flagged(), item.confidence(), item.reason(),
                            properties.flaggedThreshold()
                    ))
                    .toList();
        } catch (Exception e) {
            parseFailureCounter.increment();
            log.warn("Gemini 응답 파싱 실패, 해당 배치 skip ({}건). responseText={}",
                    requestedNicknames.size(), responseText, e);
            return List.of();
        }
    }

    private record GeminiAuditItem(String nickname, boolean flagged, double confidence, String reason) {
    }
}
