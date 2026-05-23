package coffeeshout.room.infra;

import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.room.config.PlayerNameAuditProperties;
import coffeeshout.room.domain.audit.AiConfidence;
import coffeeshout.room.domain.audit.PlayerNameAuditErrorCode;
import coffeeshout.room.domain.audit.PlayerNameAuditResult;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.domain.audit.PlayerNameAuditor;
import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackEntity;
import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!local & !test")
public class GeminiPlayerNameAuditor implements PlayerNameAuditor {

    private static final Content SYSTEM_INSTRUCTION = Content.fromParts(
            Part.fromText(PlayerNameAuditPromptTemplate.SYSTEM_INSTRUCTION)
    );

    private static final Schema RESPONSE_SCHEMA = Schema.builder()
            .type("ARRAY")
            .items(Schema.builder()
                    .type("OBJECT")
                    .properties(Map.of(
                            "playerName", Schema.builder().type("STRING").build(),
                            "flagged", Schema.builder().type("BOOLEAN").build(),
                            "confidence", Schema.builder().type("NUMBER").build(),
                            "reason", Schema.builder().type("STRING").build()
                    ))
                    .required(List.of("playerName", "flagged", "confidence", "reason"))
                    .build())
            .build();

    private final Client geminiClient;
    private final ObjectMapper objectMapper;
    private final PlayerNameAuditProperties properties;
    private final PlayerNameFeedbackJpaRepository feedbackRepository;
    private final PlayerNameAuditPromptTemplate promptTemplate;
    private final Timer apiCallTimer;
    private final Counter parseFailureCounter;
    private final Counter itemParseFailureCounter;

    public GeminiPlayerNameAuditor(
            @Qualifier("nicknameAuditClient") Client geminiClient,
            ObjectMapper objectMapper,
            PlayerNameAuditProperties properties,
            PlayerNameFeedbackJpaRepository feedbackRepository,
            PlayerNameAuditPromptTemplate promptTemplate,
            MeterRegistry meterRegistry
    ) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.feedbackRepository = feedbackRepository;
        this.promptTemplate = promptTemplate;
        this.apiCallTimer = Timer.builder("playerName.audit.gemini.call.duration")
                .description("Gemini API 호출 소요 시간")
                .register(meterRegistry);
        this.parseFailureCounter = Counter.builder("playerName.audit.gemini.parse.failures")
                .description("Gemini 응답 JSON 파싱 실패 횟수")
                .register(meterRegistry);
        this.itemParseFailureCounter = Counter.builder("playerName.audit.gemini.item.parse.failures")
                .description("Gemini 응답 항목 단위 파싱 실패 횟수")
                .register(meterRegistry);
    }

    @Override
    @Retry(name = "geminiAudit")
    @RateLimiter(name = "geminiAudit")
    public List<PlayerNameAuditResult> audit(List<String> nicknames) {
        final String userMessage = buildUserMessage(nicknames);

        final GenerateContentResponse response;
        try {
            response = apiCallTimer.recordCallable(() ->
                    geminiClient.models.generateContent(
                            properties.model(),
                            userMessage,
                            GenerateContentConfig.builder()
                                    .systemInstruction(SYSTEM_INSTRUCTION)
                                    .responseMimeType("application/json")
                                    .responseSchema(RESPONSE_SCHEMA)
                                    .build()
                    ));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new InfrastructureException(PlayerNameAuditErrorCode.AI_CALL_FAILED, "닉네임 검열 AI 호출 실패", e);
        }

        if (response == null || response.text() == null) {
            throw new InfrastructureException(PlayerNameAuditErrorCode.AI_EMPTY_RESPONSE, "닉네임 검열 AI가 빈 응답을 반환했습니다.");
        }

        return parseResults(response.text(), nicknames);
    }

    private String buildUserMessage(List<String> nicknames) {
        final List<PlayerNameFeedbackEntity> examples = loadFeedbackExamples();
        return promptTemplate.buildUserMessage(nicknames, examples);
    }

    private List<PlayerNameFeedbackEntity> loadFeedbackExamples() {
        final List<PlayerNameFeedbackEntity> feedbacks = feedbackRepository.findRecentFeedbacks(
                PageRequest.of(0, properties.feedbackInjectionThreshold(), Sort.by("createdAt").descending())
        );
        if (feedbacks.size() < properties.feedbackInjectionThreshold()) {
            return List.of();
        }
        return feedbacks;
    }

    private List<PlayerNameAuditResult> parseResults(String responseText, List<String> requestedNicknames) {
        final List<JsonNode> nodes;
        try {
            nodes = objectMapper.readValue(responseText, new TypeReference<>() {});
        } catch (Exception e) {
            parseFailureCounter.increment();
            log.warn("[PlayerNameAudit] Gemini 응답 JSON 파싱 실패 ({}건). responseText={}",
                    requestedNicknames.size(), responseText, e);
            throw new InfrastructureException(PlayerNameAuditErrorCode.AI_RESPONSE_PARSE_FAILED, "닉네임 검열 AI 응답 파싱 실패", e);
        }

        final List<PlayerNameAuditResult> results = new ArrayList<>();
        for (final JsonNode node : nodes) {
            try {
                final GeminiAuditItem item = objectMapper.treeToValue(node, GeminiAuditItem.class);
                results.add(PlayerNameAuditResult.of(
                        item.playerName(), item.flagged(), item.confidence(), item.reason(),
                        properties.flaggedThreshold()
                ));
            } catch (Exception e) {
                itemParseFailureCounter.increment();
                log.warn("[PlayerNameAudit] Gemini 응답 항목 파싱 실패, PENDING 처리. node={}", node, e);
                final String playerName = node.path("playerName").asText("unknown");
                results.add(new PlayerNameAuditResult(
                        playerName, PlayerNameAuditStatus.PENDING, AiConfidence.UNKNOWN, "응답 파싱 실패"
                ));
            }
        }
        return results;
    }

    private record GeminiAuditItem(String playerName, boolean flagged, double confidence, String reason) {}
}
