package coffeeshout.zzolbot.eval.infra;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.eval.domain.EvalVerdict;
import coffeeshout.zzolbot.eval.domain.JudgeScore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Gemini로 피평가 답변을 채점한다. 결정성을 위해 temperature/topP를 0으로 두고 JSON 응답을 강제한다.
 * 진단 봇과 레이트리밋 경합을 피하려 별도 resilience 인스턴스({@code zzolBotJudge})를 쓴다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class GeminiJudgeClient implements JudgeClient {

    private static final String SYSTEM_INSTRUCTION = """
            너는 운영 진단 봇의 답변을 채점하는 엄격한 평가자다.
            주어진 질문, 채점 기준(rubric), 봇의 답변을 보고 아래 JSON 스키마로만 응답하라.
            {
              "accuracy": 0~5 정수 (rubric의 핵심을 맞혔는가),
              "groundedness": 0~5 정수 (도구 결과/근거에 기반했는가),
              "hallucinationDetected": true/false (없는 사실·수치를 지어냈는가),
              "verdict": "PASS" 또는 "FAIL",
              "rationale": "판정 근거 한국어 한두 문장"
            }
            설명 텍스트 없이 JSON 객체 하나만 출력하라.""";

    private final @Qualifier("zzolBotClient") Client zzolBotClient;
    private final ZzolBotProperties properties;
    private final ObjectMapper objectMapper;

    @Retry(name = "zzolBotJudge")
    @RateLimiter(name = "zzolBotJudge")
    @Override
    public JudgeScore evaluate(String question, String rubric, String answer) {
        final GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(SYSTEM_INSTRUCTION)))
                .temperature(0f)
                .topP(0f)
                .responseMimeType("application/json")
                .build();
        final String prompt = buildPrompt(question, rubric, answer);
        final GenerateContentResponse response = callApi(prompt, config);
        return parse(response.text());
    }

    protected GenerateContentResponse callApi(String prompt, GenerateContentConfig config) {
        try {
            return zzolBotClient.models.generateContent(
                    properties.model(), List.of(Content.fromParts(Part.fromText(prompt))), config);
        } catch (Exception e) {
            throw new RuntimeException("judge Gemini API 호출 실패: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String question, String rubric, String answer) {
        return """
                [질문]
                %s

                [채점 기준]
                %s

                [봇의 답변]
                %s
                """.formatted(question, rubric, answer);
    }

    private JudgeScore parse(String json) {
        try {
            final JsonNode node = objectMapper.readTree(json);
            return new JudgeScore(
                    clamp(node.path("accuracy").asInt(0)),
                    clamp(node.path("groundedness").asInt(0)),
                    node.path("hallucinationDetected").asBoolean(false),
                    "PASS".equalsIgnoreCase(node.path("verdict").asText("FAIL"))
                            ? EvalVerdict.PASS : EvalVerdict.FAIL,
                    node.path("rationale").asText(""));
        } catch (Exception e) {
            log.warn("[ZzolBot] judge 응답 파싱 실패. raw={}", json, e);
            return new JudgeScore(0, 0, false, EvalVerdict.FAIL, "judge 응답 파싱 실패");
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(5, value));
    }
}
