package coffeeshout.global.zzolbot.application;

import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.domain.PiiMasker;
import coffeeshout.global.zzolbot.domain.ToolExecutionResult;
import coffeeshout.global.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
import coffeeshout.global.zzolbot.infra.ZzolBotLlmClient;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZzolBotChatService {

    private final List<ZzolBotTool> tools;
    private final ZzolBotLlmClient llmClient;
    private final ZzolBotProperties properties;
    private final ZzolBotPromptTemplate promptTemplate;
    private final PiiMasker piiMasker;

    public String ask(String question, Consumer<String> progressCallback) {
        final List<Content> conversation = initConversation(question);

        for (int i = 0; i < properties.maxLoopIterations(); i++) {
            final ZzolBotLlmResponse response = llmClient.generate(conversation, tools);

            if (response instanceof ZzolBotLlmResponse.TextResponse text) {
                log.debug("[ZzolBot] 최종 응답 완료. iterations={}", i + 1);
                return text.text();
            }

            if (response instanceof ZzolBotLlmResponse.ToolCallResponse toolCall) {
                progressCallback.accept(toolCall.toolName());
                log.debug("[ZzolBot] tool 실행. name={}, iteration={}", toolCall.toolName(), i + 1);

                conversation.add(Content.builder()
                        .role("model")
                        .parts(Part.fromFunctionCall(toolCall.toolName(), toolCall.args()))
                        .build());

                final ToolExecutionResult result = executeTool(toolCall.toolName(), toolCall.args());
                final String maskedContent = piiMasker.mask(result.content());

                conversation.add(llmClient.buildFunctionResponseContent(toolCall.toolName(), maskedContent));
            }
        }

        log.warn("[ZzolBot] maxLoopIterations 초과. question={}", question);
        return "분석이 복잡하여 완료하지 못했습니다. 질문을 더 구체적으로 해주세요.";
    }

    private List<Content> initConversation(String question) {
        final List<Content> conversation = new ArrayList<>();
        conversation.add(Content.builder()
                .role("user")
                .parts(Part.fromText(promptTemplate.build()))
                .build());
        conversation.add(Content.builder()
                .role("model")
                .parts(Part.fromText("네, zzol 운영 어시스턴트입니다. 무엇을 도와드릴까요?"))
                .build());
        conversation.add(Content.builder()
                .role("user")
                .parts(Part.fromText(question))
                .build());
        return conversation;
    }

    private ToolExecutionResult executeTool(String toolName, Map<String, Object> args) {
        return tools.stream()
                .filter(t -> t.name().equals(toolName))
                .findFirst()
                .map(t -> t.execute(args))
                .orElse(ToolExecutionResult.fail(toolName, "알 수 없는 tool: " + toolName));
    }
}
