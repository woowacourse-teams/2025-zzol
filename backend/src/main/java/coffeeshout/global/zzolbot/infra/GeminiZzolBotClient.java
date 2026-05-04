package coffeeshout.global.zzolbot.infra;

import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Tool;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class GeminiZzolBotClient {

    private final @Qualifier("zzolBotClient") Client zzolBotClient;
    private final ZzolBotProperties properties;
    private final ZzolBotSchemaConverter schemaConverter;

    @Retry(name = "zzolBotGemini")
    @RateLimiter(name = "zzolBotGemini")
    public ZzolBotLlmResponse generate(List<Content> conversation, List<ZzolBotTool> tools) {
        final GenerateContentConfig config = buildConfig(tools);
        final GenerateContentResponse response = callApi(conversation, config);
        return parseResponse(response);
    }

    protected GenerateContentResponse callApi(List<Content> conversation, GenerateContentConfig config) {
        try {
            return zzolBotClient.models.generateContent(properties.model(), conversation, config);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API 호출 실패: " + e.getMessage(), e);
        }
    }

    private GenerateContentConfig buildConfig(List<ZzolBotTool> tools) {
        final List<FunctionDeclaration> declarations = tools.stream()
                .map(this::toFunctionDeclaration)
                .toList();

        return GenerateContentConfig.builder()
                .tools(List.of(Tool.builder()
                        .functionDeclarations(declarations)
                        .build()))
                .build();
    }

    private FunctionDeclaration toFunctionDeclaration(ZzolBotTool tool) {
        return FunctionDeclaration.builder()
                .name(tool.name())
                .description(tool.description())
                .parameters(schemaConverter.convert(tool.parameterSchema()))
                .build();
    }

    private ZzolBotLlmResponse parseResponse(GenerateContentResponse response) {
        final List<FunctionCall> functionCalls = response.functionCalls();

        if (!functionCalls.isEmpty()) {
            final FunctionCall fc = functionCalls.get(0);
            final String toolName = fc.name().orElse("");
            final Map<String, Object> args = fc.args().orElse(Collections.emptyMap());
            log.debug("[ZzolBot] tool 호출 요청: toolName={}, args={}", toolName, args);
            return new ZzolBotLlmResponse.ToolCallResponse(toolName, args);
        }

        return new ZzolBotLlmResponse.TextResponse(response.text());
    }

    public Content buildFunctionResponseContent(String toolName, String result) {
        return Content.fromParts(
                Part.fromFunctionResponse(toolName, Map.of("result", result))
        );
    }
}
