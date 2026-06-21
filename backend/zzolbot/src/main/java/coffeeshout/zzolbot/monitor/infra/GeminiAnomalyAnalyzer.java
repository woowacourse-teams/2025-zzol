package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.monitor.domain.AnomalyVerdict;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import coffeeshout.zzolbot.monitor.domain.MonitorSignal;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Gemini로 이상 신호를 요약·근본원인 분석한다. 이상+예산 보유 시에만 호출되므로 호출 빈도가 낮다.
 * 조치는 제안만 생성하며 자동 실행하지 않는다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class GeminiAnomalyAnalyzer implements AnomalyAnalyzer {

    private static final String SYSTEM_INSTRUCTION = """
            너는 운영 모니터링 분석가다. 임계값을 초과한 신호 목록을 보고 아래 JSON으로만 응답하라.
            {
              "summary": "현재 상황 한국어 1~2문장 요약",
              "rootCauseHypothesis": "가장 가능성 높은 근본 원인 가설",
              "suggestedActions": ["운영자가 취할 수 있는 조치 제안", "..."]
            }
            조치는 제안일 뿐 자동 실행되지 않는다. 설명 텍스트 없이 JSON 객체 하나만 출력하라.""";

    private final @Qualifier("zzolBotClient") Client zzolBotClient;
    private final ZzolBotProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public MonitorAnalysis analyze(MonitorSnapshot snapshot, AnomalyVerdict verdict) {
        final GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(SYSTEM_INSTRUCTION)))
                .temperature(0f)
                .topP(0f)
                .responseMimeType("application/json")
                .build();
        final String prompt = buildPrompt(snapshot, verdict);
        try {
            final GenerateContentResponse response = zzolBotClient.models.generateContent(
                    properties.model(), List.of(Content.fromParts(Part.fromText(prompt))), config);
            return parse(response.text());
        } catch (Exception e) {
            log.warn("[ZzolBot] 이상 분석 LLM 호출 실패", e);
            return MonitorAnalysis.failed();
        }
    }

    private String buildPrompt(MonitorSnapshot snapshot, AnomalyVerdict verdict) {
        final StringBuilder sb = new StringBuilder();
        sb.append("심각도: ").append(verdict.severity()).append('\n');
        sb.append("초과 지문: ").append(verdict.fingerprint()).append('\n');
        sb.append("신호:\n");
        for (MonitorSignal signal : snapshot.signals()) {
            sb.append("- ").append(signal.name())
                    .append(": value=").append(signal.value())
                    .append(", threshold=").append(signal.threshold())
                    .append(", breached=").append(signal.breached())
                    .append('\n');
        }
        return sb.toString();
    }

    private MonitorAnalysis parse(String json) {
        try {
            final JsonNode node = objectMapper.readTree(json);
            final List<String> actions = new ArrayList<>();
            node.path("suggestedActions").forEach(a -> actions.add(a.asText()));
            return new MonitorAnalysis(
                    node.path("summary").asText(""),
                    node.path("rootCauseHypothesis").asText(""),
                    actions);
        } catch (Exception e) {
            log.warn("[ZzolBot] 이상 분석 응답 파싱 실패. raw={}", json, e);
            return MonitorAnalysis.failed();
        }
    }
}
