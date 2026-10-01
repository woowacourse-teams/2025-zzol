package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.zzolbot.config.ZzolBotHttpTimeouts;
import coffeeshout.zzolbot.domain.ZzolBotErrorCode;
import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 자체 호스팅 모델(llama.cpp 서버)을 호출한다. 섀도우 경로 전용이라 운영 판정에는 쓰이지 않는다.
 *
 * <p><b>인용을 문법으로 강제한다.</b> API 뒤에 있는 모델과 달리 로컬 모델은 디코딩에 개입할 수 있다.
 * 없는 로그를 인용하지 말라고 <i>부탁</i>하는 대신 <i>불가능</i>하게 만든다({@link CitationGrammar}).
 *
 * <p><b>샘플링 파라미터를 전부 명시한다.</b> 명시하지 않으면 서버 기본값이 조용히 적용되어, 같은
 * 그리디라고 부른 것이 서로 다른 것이 된다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "zzol-bot.monitor.shadow", name = "enabled", havingValue = "true")
public class LlamaCppShadowClient implements ShadowModelClient {

    private final RestClient restClient;
    private final MonitorAnalysisContract contract;
    private final int maxTokens;

    public LlamaCppShadowClient(
            MonitorProperties properties, RestClient.Builder restClientBuilder, MonitorAnalysisContract contract) {
        this.restClient = restClientBuilder
                .baseUrl(properties.shadow().baseUrl())
                .requestFactory(ZzolBotHttpTimeouts.requestFactory(
                        properties.shadow().connectTimeout(),
                        properties.shadow().readTimeout()))
                .build();
        this.contract = contract;
        this.maxTokens = properties.shadow().maxTokens();
    }

    @Override
    public String generate(FiringAlert alert, List<String> logSamples, String logEnvironment) {
        // 본문을 문자열로 받아 직접 파싱한다. 프레임워크 컨버터에 맡기면 두 군데서 깨진다.
        // Jackson 버전이 어긋나고(#1813), 모델이 옮겨 적은 제어문자를 거부한다(#1811).
        final String raw = restClient
                .post()
                .uri("/v1/chat/completions")
                .body(requestBody(alert, logSamples, logEnvironment))
                .retrieve()
                .body(String.class);
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ZzolBotErrorCode.SHADOW_MODEL_RESPONSE_INVALID, "자체 모델 응답 본문이 비어 있습니다.");
        }
        return content(raw);
    }

    private String content(String raw) {
        try {
            return TolerantJson.readTree(raw)
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("");
        } catch (IOException e) {
            throw new BusinessException(ZzolBotErrorCode.SHADOW_MODEL_RESPONSE_INVALID, "자체 모델 응답 봉투를 읽지 못했습니다.");
        }
    }

    private Map<String, Object> requestBody(FiringAlert alert, List<String> logSamples, String logEnvironment) {
        return Map.ofEntries(
                Map.entry(
                        "messages",
                        List.of(
                                Map.of("role", "system", "content", MonitorAnalysisContract.SYSTEM_INSTRUCTION),
                                Map.of(
                                        "role",
                                        "user",
                                        "content",
                                        contract.buildPrompt(alert, logSamples, logEnvironment)))),
                Map.entry("grammar", CitationGrammar.forLogSamples(logSamples)),
                // 네이티브 키와 OpenAI 호환 키를 둘 다 보낸다. 지금 서버는 둘 다 먹지만
                // 버전이 올라가면 한쪽만 남을 수 있고, 무시되면 생성이 안 끊긴다
                Map.entry("n_predict", maxTokens),
                Map.entry("max_tokens", maxTokens),
                Map.entry("temperature", 0.0),
                Map.entry("top_k", 0),
                Map.entry("top_p", 1.0),
                Map.entry("min_p", 0.0),
                Map.entry("repeat_penalty", 1.0),
                Map.entry("presence_penalty", 0.0),
                Map.entry("frequency_penalty", 0.0),
                Map.entry("cache_prompt", false));
    }
}
