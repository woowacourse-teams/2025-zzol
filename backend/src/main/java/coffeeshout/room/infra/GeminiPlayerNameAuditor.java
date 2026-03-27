package coffeeshout.room.infra;

import coffeeshout.room.config.PlayerNameAuditProperties;
import coffeeshout.room.domain.audit.PlayerNameAuditResult;
import coffeeshout.room.domain.audit.PlayerNameAuditor;
import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackEntity;
import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Map;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
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
public class GeminiPlayerNameAuditor implements PlayerNameAuditor {

    private static final String BASE_SYSTEM_PROMPT = """
            너는 한국어 닉네임 검열 전문가다.
            아래 닉네임 목록을 검토하고 각 항목에 대해 JSON 배열로만 응답하라. 반드시 다른 텍스트는 포함하지 마라.
            
            비속어 판단 기준:
            - 직접적 욕설뿐 아니라 자모 분리, 특수문자 삽입, 유사 발음 대체로 우회한 경우 포함
            - 문화적 맥락을 고려한다 (예: "미쳤다"는 일반 감탄사로 사용되므로 flagged=false)
            - 판단이 애매한 경우 confidence를 낮게 설정한다
            
            응답 형식:
            [
              { "playerName": "씨b알",     "flagged": true,  "confidence": 0.97, "reason": "비속어 우회 (특수문자 삽입)" },
              { "playerName": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "일반 닉네임" }
            ]
            """;

    private final Client geminiClient;
    private final ObjectMapper objectMapper;
    private final PlayerNameAuditProperties properties;
    private final PlayerNameFeedbackJpaRepository feedbackRepository;
    private final MeterRegistry meterRegistry;

    private Timer apiCallTimer;
    private Counter parseFailureCounter;
    private Counter itemParseFailureCounter;

    @PostConstruct
    void initMetrics() {
        apiCallTimer = Timer.builder("playerName.audit.gemini.call.duration")
                .description("Gemini API 호출 소요 시간")
                .register(meterRegistry);
        parseFailureCounter = Counter.builder("playerName.audit.gemini.parse.failures")
                .description("Gemini 응답 JSON 파싱 실패 횟수")
                .register(meterRegistry);
        itemParseFailureCounter = Counter.builder("playerName.audit.gemini.item.parse.failures")
                .description("Gemini 응답 항목 단위 파싱 실패 횟수")
                .register(meterRegistry);
    }

    @Override
    @Retry(name = "geminiAudit")
    @RateLimiter(name = "geminiAudit")
    public List<PlayerNameAuditResult> audit(List<String> nicknames) {
        final String prompt = buildPrompt(nicknames);

        try {
            final GenerateContentResponse response = apiCallTimer.recordCallable(() ->
                    geminiClient.models.generateContent(
                            properties.model(),
                            prompt,
                            GenerateContentConfig.builder()
                                    .responseMimeType("application/json")
                                    .build()
                    ));

            if (response == null) {
                return List.of();
            }

            return parseResults(response.text(), nicknames);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API 호출 실패", e);
        }
    }

    private String buildPrompt(List<String> nicknames) {
        StringBuilder prompt = new StringBuilder(BASE_SYSTEM_PROMPT);

        List<PlayerNameFeedbackEntity> feedbacks = feedbackRepository.findRecentFeedbacks(
                PageRequest.of(0, properties.feedbackInjectionThreshold(), Sort.by("createdAt").descending())
        );

        if (feedbacks.size() >= properties.feedbackInjectionThreshold()) {
            List<Map<String, Object>> examples = feedbacks.stream()
                    .map(feedback -> {
                        boolean operatorFlagged =
                                feedback.getOperatorDecision() == PlayerNameFeedbackEntity.OperatorDecision.BLOCKED;
                        double exampleConfidence = operatorFlagged ? 0.99 : 0.01;
                        return Map.<String, Object>of(
                                "playerName", feedback.getPlayerName(),
                                "flagged", operatorFlagged,
                                "confidence", exampleConfidence,
                                "reason", "운영자 피드백"
                        );
                    })
                    .toList();
            try {
                prompt.append("\n운영자 피드백 기반 추가 예시:\n")
                        .append(objectMapper.writeValueAsString(examples)).append("\n");
            } catch (JsonProcessingException e) {
                throw new RuntimeException("피드백 예시 직렬화 실패", e);
            }
        }

        try {
            prompt.append("\n검열할 닉네임 목록:\n").append(objectMapper.writeValueAsString(nicknames));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("닉네임 목록 직렬화 실패", e);
        }
        return prompt.toString();
    }

    private List<PlayerNameAuditResult> parseResults(String responseText, List<String> requestedNicknames) {
        List<JsonNode> nodes;
        try {
            nodes = objectMapper.readValue(responseText, new TypeReference<>() {});
        } catch (Exception e) {
            parseFailureCounter.increment();
            log.warn("Gemini 응답 JSON 파싱 실패, 해당 배치 skip ({}건). responseText={}",
                    requestedNicknames.size(), responseText, e);
            return List.of();
        }

        List<PlayerNameAuditResult> results = new ArrayList<>();
        for (JsonNode node : nodes) {
            try {
                GeminiAuditItem item = objectMapper.treeToValue(node, GeminiAuditItem.class);
                results.add(PlayerNameAuditResult.of(
                        item.playerName(), item.flagged(), item.confidence(), item.reason(),
                        properties.flaggedThreshold()
                ));
            } catch (Exception e) {
                itemParseFailureCounter.increment();
                log.warn("Gemini 응답 항목 파싱 실패, 해당 항목 skip. node={}", node, e);
            }
        }
        return results;
    }

    private record GeminiAuditItem(String playerName, boolean flagged, double confidence, String reason) {
    }
}
