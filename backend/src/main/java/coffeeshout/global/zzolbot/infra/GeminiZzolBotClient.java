package coffeeshout.global.zzolbot.infra;

import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.domain.AskContext;
import coffeeshout.global.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.global.zzolbot.domain.ZzolBotMessage;
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
@Profile("!test")
@RequiredArgsConstructor
public class GeminiZzolBotClient implements ZzolBotLlmClient {

    private final @Qualifier("zzolBotClient") Client zzolBotClient;
    private final ZzolBotProperties properties;
    private final ZzolBotSchemaConverter schemaConverter;

    @Retry(name = "zzolBotGemini")
    @RateLimiter(name = "zzolBotGemini")
    public ZzolBotLlmResponse generate(List<ZzolBotMessage> conversation, List<ZzolBotTool> tools, String systemInstruction, AskContext ctx) {
        final GenerateContentConfig config = buildConfig(tools, systemInstruction, ctx);
        final List<Content> contents = conversation.stream().map(this::toContent).toList();
        final GenerateContentResponse response = callApi(contents, config);
        return parseResponse(response);
    }

    protected GenerateContentResponse callApi(List<Content> contents, GenerateContentConfig config) {
        try {
            return zzolBotClient.models.generateContent(properties.model(), contents, config);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API 호출 실패: " + e.getMessage(), e);
        }
    }

    private GenerateContentConfig buildConfig(List<ZzolBotTool> tools, String systemInstruction, AskContext ctx) {
        final List<FunctionDeclaration> declarations = tools.stream()
                .map(this::toFunctionDeclaration)
                .toList();

        return GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                .tools(List.of(Tool.builder()
                        .functionDeclarations(declarations)
                        .build()))
                .temperature((float) properties.determinism().temperature())
                .topP((float) properties.determinism().topP())
                .seed((int) (ctx.seed() & Integer.MAX_VALUE))
                .build();
    }

    private Content toContent(ZzolBotMessage message) {
        return switch (message) {
            case ZzolBotMessage.UserMessage m ->
                    Content.builder().role("user").parts(Part.fromText(m.text())).build();
            case ZzolBotMessage.AssistantMessage m ->
                    Content.builder().role("model").parts(Part.fromText(m.text())).build();
            case ZzolBotMessage.ToolCallMessage m ->
                    Content.builder().role("model").parts(Part.fromFunctionCall(m.toolName(), m.args())).build();
            case ZzolBotMessage.ToolResultMessage m ->
                    Content.fromParts(Part.fromFunctionResponse(m.toolName(), Map.of("result", m.result())));
        };
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
            final List<ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem> calls = functionCalls.stream()
                    .map(fc -> new ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem(
                            fc.name().orElse(""),
                            fc.args().orElse(Collections.emptyMap())
                    ))
                    .toList();
            log.debug("[ZzolBot] tool 호출 요청: count={}, tools={}", calls.size(),
                    calls.stream().map(ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem::toolName).toList());
            return new ZzolBotLlmResponse.ToolCallsResponse(calls);
        }

        return new ZzolBotLlmResponse.TextResponse(response.text());
    }
}
