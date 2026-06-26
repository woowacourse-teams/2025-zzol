package coffeeshout.zzolbot.remediation.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Gemini로 결함 수정을 제안한다. {@code GeminiAnomalyAnalyzer}와 같은 결정적 구조 출력 패턴(temperature 0,
 * responseMimeType=application/json)을 따르되, GitHub Actions 워커에서 CLI로 직접 인스턴스화되므로
 * Spring 빈이 아니다. API 호출은 테스트에서 가로채도록 {@link #callApi}를 protected로 노출한다.
 *
 * <p>토큰 최소화: 전체 repo가 아니라 특정된 대상 파일 한 개만 프롬프트에 넣는다. 모델 선택이 아니라 컨텍스트
 * 스코핑이 비용을 가른다.
 */
@Slf4j
public class GeminiCodingAgent implements CodingAgent {

    private static final String SYSTEM_INSTRUCTION = """
            너는 신중한 백엔드 엔지니어다. 주어진 단일 파일의 결함(주로 NPE/빈 Optional 역참조)을 최소 변경으로
            고치고, 그 결함을 재현하는 JUnit5 테스트를 작성한다. 아래 JSON으로만 응답하라.
            {
              "modifiedSource": "대상 파일의 수정된 전체 소스(패키지·임포트 포함)",
              "reproTestPath": "repo 루트 기준 테스트 파일 경로(.../src/test/java/...)",
              "reproTestSource": "재현 테스트 전체 소스 — 수정 전 실패(RED), 수정 후 통과(GREEN)해야 한다",
              "rationale": "무엇을 왜 고쳤는지 1~2문장"
            }
            규칙: 요청된 결함만 최소 변경으로 고친다. 무관한 리팩터링·포맷 변경 금지. 동작을 보존한다.
            재현 테스트는 외부 의존(DB·네트워크) 없이 단위 수준으로 작성한다. 설명 텍스트 없이 JSON 하나만 출력하라.""";

    private final Client client;
    private final String model;
    private final ObjectMapper objectMapper;

    public GeminiCodingAgent(Client client, String model, ObjectMapper objectMapper) {
        this.client = client;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    @Override
    public PatchProposal propose(DefectContext context) {
        final GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(SYSTEM_INSTRUCTION)))
                .temperature(0f)
                .topP(0f)
                .responseMimeType("application/json")
                .build();
        final GenerateContentResponse response = callApi(buildPrompt(context), config);
        return parse(context, response.text());
    }

    protected GenerateContentResponse callApi(String prompt, GenerateContentConfig config) {
        try {
            return client.models.generateContent(
                    model, List.of(Content.fromParts(Part.fromText(prompt))), config);
        } catch (Exception e) {
            throw new RuntimeException("수정 제안 Gemini API 호출 실패: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(DefectContext context) {
        final DefectLocation loc = context.location();
        return new StringBuilder()
                .append("결함 유형: ").append(context.defectType()).append('\n')
                .append("근본원인 가설: ").append(safe(context.rootCauseHypothesis())).append('\n')
                .append("결함 위치: ").append(loc.filePath())
                .append(" (").append(loc.classFqn()).append('#').append(loc.methodName())
                .append(", line ").append(loc.lineNumber()).append(")\n")
                .append("\n스택트레이스:\n").append(safe(context.stackTrace())).append('\n')
                .append("\n대상 파일 전체 소스(").append(loc.filePath()).append("):\n")
                .append(context.targetSource())
                .toString();
    }

    PatchProposal parse(DefectContext context, String json) {
        try {
            final JsonNode node = objectMapper.readTree(json);
            return new PatchProposal(
                    context.location().filePath(),
                    node.path("modifiedSource").asText(""),
                    node.path("reproTestPath").asText(""),
                    node.path("reproTestSource").asText(""),
                    node.path("rationale").asText(""));
        } catch (Exception e) {
            // 모델이 깨진 JSON을 반환할 수 있다(특히 큰 파일). 크래시 대신 빈 제안을 돌려
            // 호출측이 NO_FIX로 깔끔히 떨구게 한다 — 봇은 틀렸을 때 멈춰야지 터지면 안 된다.
            log.warn("[ZzolBot] 수정 제안 응답 파싱 실패 — NO_FIX 처리. {}", e.getMessage());
            return new PatchProposal(context.location().filePath(), "", "", "", "");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
