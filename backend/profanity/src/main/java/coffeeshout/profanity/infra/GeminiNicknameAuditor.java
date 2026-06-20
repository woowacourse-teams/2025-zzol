package coffeeshout.profanity.infra;

import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAuditErrorCode;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import coffeeshout.profanity.domain.audit.NicknameFeedback;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
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
public class GeminiNicknameAuditor implements NicknameAuditor {

    private static final Content SYSTEM_INSTRUCTION = Content.fromParts(
            Part.fromText(NicknameAuditPromptTemplate.SYSTEM_INSTRUCTION)
    );

    private static final Schema RESPONSE_SCHEMA = Schema.builder()
            .type("ARRAY")
            .items(Schema.builder()
                    .type("OBJECT")
                    .properties(Map.of(
                            "nickname", Schema.builder().type("STRING").build(),
                            "flagged", Schema.builder().type("BOOLEAN").build(),
                            "confidence", Schema.builder().type("NUMBER").build(),
                            "reason", Schema.builder().type("STRING").build()
                    ))
                    .required(List.of("nickname", "flagged", "confidence", "reason"))
                    .build())
            .build();

    private final Client geminiClient;
    private final ObjectMapper objectMapper;
    private final NicknameAuditProperties properties;
    private final NicknameFeedbackRepository feedbackRepository;
    private final NicknameAuditPromptTemplate promptTemplate;
    private final Timer apiCallTimer;
    private final Counter parseFailureCounter;
    private final Counter itemParseFailureCounter;

    public GeminiNicknameAuditor(
            @Qualifier("nicknameAuditClient") Client geminiClient,
            ObjectMapper objectMapper,
            NicknameAuditProperties properties,
            NicknameFeedbackRepository feedbackRepository,
            NicknameAuditPromptTemplate promptTemplate,
            MeterRegistry meterRegistry
    ) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.feedbackRepository = feedbackRepository;
        this.promptTemplate = promptTemplate;
        this.apiCallTimer = Timer.builder("nickname.audit.gemini.call.duration")
                .description("Gemini API 호출 소요 시간")
                .register(meterRegistry);
        this.parseFailureCounter = Counter.builder("nickname.audit.gemini.parse.failures")
                .description("Gemini 응답 JSON 파싱 실패 횟수")
                .register(meterRegistry);
        this.itemParseFailureCounter = Counter.builder("nickname.audit.gemini.item.parse.failures")
                .description("Gemini 응답 항목 단위 파싱 실패 횟수")
                .register(meterRegistry);
    }

    @Override
    @Retry(name = "geminiAudit")
    @RateLimiter(name = "geminiAudit")
    public List<NicknameAuditResult> audit(List<String> nicknames) {
        final String userMessage = buildUserMessage(nicknames);
        final GenerateContentResponse response = callWithModelFallback(userMessage);

        if (response == null || response.text() == null) {
            throw new InfrastructureException(NicknameAuditErrorCode.AI_EMPTY_RESPONSE, "닉네임 검열 AI가 빈 응답을 반환했습니다.");
        }

        return parseResults(response.text(), nicknames);
    }

    /**
     * 모델 목록을 우선순위 순으로 호출한다. 요청 한도(429)에 걸린 모델은 건너뛰고 다음 모델로 폴백한다.
     * 무료 등급의 RPM/RPD 한도는 프로젝트·모델 단위로 부과되므로, 모델을 바꾸면 별도 한도 버킷을 사용한다.
     * 한도 외의 오류(400 등 결정론적 실패)는 모델을 바꿔도 동일하게 실패하므로 즉시 중단한다.
     */
    private GenerateContentResponse callWithModelFallback(String userMessage) {
        final List<String> models = properties.models();
        for (int i = 0; i < models.size(); i++) {
            final String model = models.get(i);
            try {
                return apiCallTimer.recordCallable(() ->
                        geminiClient.models.generateContent(
                                model,
                                userMessage,
                                GenerateContentConfig.builder()
                                        .systemInstruction(SYSTEM_INSTRUCTION)
                                        .responseMimeType("application/json")
                                        .responseSchema(RESPONSE_SCHEMA)
                                        .build()
                        ));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InfrastructureException(NicknameAuditErrorCode.AI_CALL_FAILED, "닉네임 검열 AI 호출 중 인터럽트", e);
            } catch (Exception e) {
                if (!shouldFallback(e)) {
                    throw new InfrastructureException(NicknameAuditErrorCode.AI_CALL_FAILED, "닉네임 검열 AI 호출 실패", e);
                }
                if (i == models.size() - 1) {
                    throw new InfrastructureException(NicknameAuditErrorCode.AI_RATE_LIMIT_EXHAUSTED, "모든 Gemini 모델 호출 실패(요청 한도 또는 모델 없음)", e);
                }
                log.warn("[NicknameAudit] 모델 {} 호출 불가(429/404), 폴백 → {}", model, models.get(i + 1));
            }
        }
        throw new InfrastructureException(NicknameAuditErrorCode.AI_RATE_LIMIT_EXHAUSTED, "모든 Gemini 모델 호출 실패(요청 한도 또는 모델 없음)");
    }

    /**
     * 다음 모델로 폴백해야 하는 오류인지 판별한다.
     * <ul>
     *   <li>429(RESOURCE_EXHAUSTED): 요청 한도 초과 — 다른 모델은 별도 한도 버킷을 가진다</li>
     *   <li>404(NOT_FOUND): 모델이 없거나 미제공 — 모델 단위 문제이므로 다음 모델을 시도한다</li>
     * </ul>
     * 그 외(400 등 전역·결정론적 오류, 5xx)는 모델을 바꿔도 동일하게 실패하므로 폴백하지 않는다.
     * 우선 SDK의 {@link ApiException#code()}로 식별하고, 래핑 등으로 타입을 잃은 경우 메시지로 보조 판별한다.
     */
    boolean shouldFallback(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ApiException apiException && (apiException.code() == 429 || apiException.code() == 404)) {
                return true;
            }
            final String message = t.getMessage();
            if (message != null && (
                    message.contains("RESOURCE_EXHAUSTED")
                            || message.contains("Too Many Requests")
                            || message.contains("rateLimitExceeded")
                            || message.contains("Quota exceeded")
                            || message.contains("NOT_FOUND"))) {
                return true;
            }
        }
        return false;
    }

    private String buildUserMessage(List<String> nicknames) {
        final List<NicknameFeedback> examples = loadFeedbackExamples();
        return promptTemplate.buildUserMessage(nicknames, examples);
    }

    private List<NicknameFeedback> loadFeedbackExamples() {
        final List<NicknameFeedback> feedbacks = feedbackRepository.findRecentFeedbacks(
                PageRequest.of(0, properties.feedbackInjectionThreshold(), Sort.by("createdAt").descending())
        );
        if (feedbacks.size() < properties.feedbackInjectionThreshold()) {
            return List.of();
        }
        return feedbacks;
    }

    private List<NicknameAuditResult> parseResults(String responseText, List<String> requestedNicknames) {
        final List<JsonNode> nodes;
        try {
            nodes = objectMapper.readValue(responseText, new TypeReference<>() {});
        } catch (Exception e) {
            parseFailureCounter.increment();
            log.warn("[NicknameAudit] Gemini 응답 JSON 파싱 실패 ({}건). responseText={}",
                    requestedNicknames.size(), responseText, e);
            throw new InfrastructureException(NicknameAuditErrorCode.AI_RESPONSE_PARSE_FAILED, "닉네임 검열 AI 응답 파싱 실패", e);
        }

        if (nodes.size() != requestedNicknames.size()) {
            log.error("[NicknameAudit] 응답 항목 수 불일치: expected={}, actual={}",
                    requestedNicknames.size(), nodes.size());
        }

        final int processCount = Math.min(nodes.size(), requestedNicknames.size());
        final List<NicknameAuditResult> results = new ArrayList<>();

        for (int i = 0; i < processCount; i++) {
            final JsonNode node = nodes.get(i);
            try {
                final GeminiAuditItem item = objectMapper.treeToValue(node, GeminiAuditItem.class);
                results.add(NicknameAuditResult.of(
                        item.nickname(), item.flagged(), item.confidence(), item.reason(),
                        properties.flaggedThreshold()
                ));
            } catch (Exception e) {
                itemParseFailureCounter.increment();
                log.warn("[NicknameAudit] Gemini 응답 항목 파싱 실패, PENDING 처리. index={}, node={}", i, node, e);
                results.add(new NicknameAuditResult(
                        requestedNicknames.get(i), NicknameAuditStatus.PENDING, AiConfidence.UNKNOWN, "응답 파싱 실패"
                ));
            }
        }

        for (int i = processCount; i < requestedNicknames.size(); i++) {
            log.warn("[NicknameAudit] 응답 누락 닉네임 PENDING 처리. index={}, nickname={}", i, requestedNicknames.get(i));
            results.add(new NicknameAuditResult(
                    requestedNicknames.get(i), NicknameAuditStatus.PENDING, AiConfidence.UNKNOWN, "응답 항목 수 불일치"
            ));
        }

        return results;
    }

    private record GeminiAuditItem(String nickname, boolean flagged, double confidence, String reason) {}
}
