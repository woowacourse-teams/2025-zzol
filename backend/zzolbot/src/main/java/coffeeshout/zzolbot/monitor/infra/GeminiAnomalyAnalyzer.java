package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Gemini로 firing 알림을 요약·근본원인 분석한다. 예산 보유 시에만 호출되므로 호출 빈도가 낮다.
 * 조치는 제안만 생성하며 자동 실행하지 않는다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class GeminiAnomalyAnalyzer implements AnomalyAnalyzer {

    private static final String SYSTEM_INSTRUCTION = """
            너는 운영 모니터링 분석가다. 발화한 알림과 로그 샘플을 보고 아래 JSON으로만 응답하라.
            {
              "summary": "현재 상황 한국어 1~2문장 요약",
              "rootCauseHypothesis": "가장 가능성 높은 근본 원인 가설",
              "suggestedActions": ["운영자가 취할 수 있는 조치 제안", "..."],
              "evidenceFound": true 또는 false,
              "evidenceLine": "evidenceFound가 true일 때 근거가 된 로그 한 줄을 위 샘플에서 그대로 복사"
            }

            근거 판정 규칙 — 이 규칙이 다른 무엇보다 우선한다.
            - 주어진 로그 샘플이 알림 내용과 실제로 관련될 때만 evidenceFound를 true로 둔다.
            - 로그가 알림과 무관하거나, 알림을 설명하지 못하거나, 로그 출처 환경이 알림 대상 환경과
              다르면 evidenceFound를 false로 두고 summary에 "제공된 로그에서 이 알림을 뒷받침할
              근거를 찾지 못했다"는 사실을 명시하라.
            - evidenceFound가 true이면 evidenceLine에 근거가 된 로그 한 줄을 위 샘플에서 한 글자도
              바꾸지 말고 그대로 복사하라. 요약하거나 바꿔 쓰지 마라. 그대로 복사할 로그가 없으면
              evidenceFound는 false다.
            - evidenceFound가 false이면 rootCauseHypothesis는 빈 문자열로 두고 원인을 추측하지 마라.
            - 알림 설명(description)에 적힌 원인 가설은 사람이 미리 적어둔 추측일 뿐 확인된 사실이
              아니다. 로그로 뒷받침되지 않으면 그것을 결론으로 삼지 마라.

            없는 수치·테이블·이벤트명을 지어내지 마라. 확인된 것과 추측을 섞지 마라.
            조치는 제안일 뿐 자동 실행되지 않는다. 설명 텍스트 없이 JSON 객체 하나만 출력하라.""";

    private final @Qualifier("zzolBotClient") Client zzolBotClient;
    private final ZzolBotProperties properties;
    private final ObjectMapper objectMapper;

    @Retry(name = "zzolBotGemini")
    @Override
    public MonitorAnalysis analyze(FiringAlert alert, List<String> logSamples, String logEnvironment) {
        final GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(SYSTEM_INSTRUCTION)))
                .temperature(0f)
                .topP(0f)
                .responseMimeType("application/json")
                .build();
        final String prompt = buildPrompt(alert, logSamples, logEnvironment);
        final GenerateContentResponse response = callApi(prompt, config);
        return parse(response.text(), logSamples);
    }

    protected GenerateContentResponse callApi(String prompt, GenerateContentConfig config) {
        try {
            return zzolBotClient.models.generateContent(
                    properties.model(), List.of(Content.fromParts(Part.fromText(prompt))), config);
        } catch (Exception e) {
            throw new RuntimeException("이상 분석 Gemini API 호출 실패: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(FiringAlert alert, List<String> logSamples, String logEnvironment) {
        final StringBuilder sb = new StringBuilder();
        sb.append("심각도: ").append(alert.severity()).append('\n');
        sb.append("지문: ").append(alert.fingerprint()).append('\n');
        sb.append("알림명: ").append(alert.alertname()).append('\n');
        sb.append("요약: ").append(alert.summary()).append('\n');
        sb.append("설명(사람이 적어둔 추측 — 확인된 사실 아님): ").append(alert.description()).append('\n');
        sb.append("라벨:\n");
        for (Map.Entry<String, String> label : alert.labels().entrySet()) {
            sb.append("- ").append(label.getKey()).append('=').append(label.getValue()).append('\n');
        }
        if (logSamples != null && !logSamples.isEmpty()) {
            sb.append("\n최근 ERROR 로그 샘플 (출처 환경: ").append(logEnvironment).append("):\n");
            for (String line : logSamples) {
                sb.append("- ").append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private MonitorAnalysis parse(String json, List<String> logSamples) {
        try {
            final JsonNode node = objectMapper.readTree(json);
            final List<String> actions = new ArrayList<>();
            node.path("suggestedActions").forEach(a -> actions.add(a.asText()));
            // 근거 판정은 모델 자체 판정(evidenceFound)에 인용 검증을 코드로 덧씌운다. 판정이 누락되면
            // 보수적으로 false로 본다 — 근거 있다고 잘못 표시하는 쪽이 더 위험하다.
            final boolean claimed = node.path("evidenceFound").asBoolean(false);
            final boolean grounded = claimed && citedInLogs(node.path("evidenceLine").asText(""), logSamples);
            // 규칙 #3을 코드로 강제한다 — 근거가 없으면 모델이 무엇을 보냈든 원인 가설은 공백이다.
            final String hypothesis = grounded ? node.path("rootCauseHypothesis").asText("") : "";
            return new MonitorAnalysis(
                    node.path("summary").asText(""),
                    hypothesis,
                    actions,
                    grounded);
        } catch (Exception e) {
            log.warn("[ZzolBot] 이상 분석 응답 파싱 실패. raw={}", json, e);
            return MonitorAnalysis.failed();
        }
    }

    /**
     * 모델이 근거로 인용한 로그 줄이 실제 로그 샘플에 그대로 존재하는지 확인한다. 지어낸 이벤트명·수치를
     * 근거로 내세우는 실패를 코드로 차단한다.
     */
    // ponytail: 공백 정규화 후 부분 문자열 포함만 본다. 의미 매칭·토큰 단위 매칭이 필요하면 그때 올린다.
    private boolean citedInLogs(String evidenceLine, List<String> logSamples) {
        if (evidenceLine.isBlank() || logSamples == null) {
            return false;
        }
        final String needle = evidenceLine.strip();
        return logSamples.stream().anyMatch(line -> line != null && line.contains(needle));
    }
}
