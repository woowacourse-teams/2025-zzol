package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Gemini로 firing 알림을 요약·근본원인 분석한다. 예산 보유 시에만 호출되므로 호출 빈도가 낮다.
 * 조치는 제안만 생성하며 자동 실행하지 않는다.
 *
 * <p>프롬프트와 응답 파싱은 {@link MonitorAnalysisContract}가 갖는다. 이 클래스에 남은 것은
 * Gemini API 호출뿐이다.
 */
@Slf4j
@Component
@Profile("!test")
@Qualifier(AnomalyAnalyzer.AUTHORITATIVE)
@RequiredArgsConstructor
public class GeminiAnomalyAnalyzer implements AnomalyAnalyzer {

    private final @Qualifier("zzolBotClient") Client zzolBotClient;
    private final ZzolBotProperties properties;
    private final MonitorAnalysisContract contract;

    @Retry(name = "zzolBotGemini")
    @Override
    public MonitorAnalysis analyze(FiringAlert alert, List<String> logSamples, String logEnvironment) {
        final GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(MonitorAnalysisContract.SYSTEM_INSTRUCTION)))
                .temperature(0f)
                .topP(0f)
                .responseMimeType("application/json")
                .build();
        final String prompt = contract.buildPrompt(alert, logSamples, logEnvironment);
        final GenerateContentResponse response = callApi(prompt, config);
        return contract.parse(response.text(), logSamples);
    }

    protected GenerateContentResponse callApi(String prompt, GenerateContentConfig config) {
        try {
            return zzolBotClient.models.generateContent(
                    properties.model(), List.of(Content.fromParts(Part.fromText(prompt))), config);
        } catch (Exception e) {
            throw new RuntimeException("이상 분석 Gemini API 호출 실패: " + e.getMessage(), e);
        }
    }
}
