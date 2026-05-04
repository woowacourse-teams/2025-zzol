package coffeeshout.global.zzolbot.application;

import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.domain.PiiMasker;
import coffeeshout.global.zzolbot.domain.ToolExecutionResult;
import coffeeshout.global.zzolbot.domain.ZzolBotChatResult;
import coffeeshout.global.zzolbot.domain.ZzolBotFeedback;
import coffeeshout.global.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.global.zzolbot.domain.ZzolBotTool;
import coffeeshout.global.zzolbot.infra.ZzolBotLlmClient;
import coffeeshout.global.zzolbot.infra.ZzolBotSessionEntity;
import coffeeshout.global.zzolbot.infra.ZzolBotSessionRepository;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ZzolBotChatService {

    private static final int FEEDBACK_EXAMPLE_LIMIT = 5;

    private final Map<String, ZzolBotTool> toolsByName;
    private final ZzolBotLlmClient llmClient;
    private final ZzolBotProperties properties;
    private final ZzolBotPromptTemplate promptTemplate;
    private final PiiMasker piiMasker;
    private final ZzolBotSessionRepository sessionRepository;

    public ZzolBotChatService(
            List<ZzolBotTool> tools,
            ZzolBotLlmClient llmClient,
            ZzolBotProperties properties,
            ZzolBotPromptTemplate promptTemplate,
            PiiMasker piiMasker,
            ZzolBotSessionRepository sessionRepository
    ) {
        this.toolsByName = tools.stream()
                .collect(Collectors.toUnmodifiableMap(ZzolBotTool::name, t -> t));
        this.llmClient = llmClient;
        this.properties = properties;
        this.promptTemplate = promptTemplate;
        this.piiMasker = piiMasker;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public ZzolBotChatResult ask(String question, String adminUsername, Consumer<String> progressCallback) {
        final List<Content> conversation = initConversation(question);

        for (int i = 0; i < properties.maxLoopIterations(); i++) {
            final ZzolBotLlmResponse response = llmClient.generate(conversation, List.copyOf(toolsByName.values()));

            if (response instanceof ZzolBotLlmResponse.TextResponse text) {
                log.debug("[ZzolBot] 최종 응답 완료. iterations={}", i + 1);
                return saveSession(question, text.text(), adminUsername);
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
        final String fallback = "분석이 복잡하여 완료하지 못했습니다. 질문을 더 구체적으로 해주세요.";
        return saveSession(question, fallback, adminUsername);
    }

    @Transactional
    public void applyFeedback(Long sessionId, ZzolBotFeedback feedback) {
        sessionRepository.findById(sessionId).ifPresent(session -> session.applyFeedback(feedback));
    }

    @Transactional(readOnly = true)
    public List<ZzolBotSessionEntity> getRecentSessions() {
        return sessionRepository.findTop20ByOrderByCreatedAtDesc();
    }

    private ZzolBotChatResult saveSession(String question, String answer, String adminUsername) {
        final ZzolBotSessionEntity session = sessionRepository.save(
                ZzolBotSessionEntity.create(question, answer, adminUsername)
        );
        return new ZzolBotChatResult(session.getId(), answer);
    }

    private List<Content> initConversation(String question) {
        final List<Content> conversation = new ArrayList<>();
        conversation.add(Content.builder()
                .role("user")
                .parts(Part.fromText(buildPromptWithFeedback()))
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

    private String buildPromptWithFeedback() {
        final StringBuilder prompt = new StringBuilder(promptTemplate.build());

        final List<ZzolBotSessionEntity> goodExamples = sessionRepository.findByFeedbackOrderByCreatedAtDesc(
                ZzolBotFeedback.GOOD, PageRequest.of(0, FEEDBACK_EXAMPLE_LIMIT)
        );

        if (!goodExamples.isEmpty()) {
            prompt.append("\n## 운영자가 좋은 진단으로 평가한 예시\n");
            goodExamples.forEach(example -> prompt
                    .append("\n질문: ").append(example.getQuestion())
                    .append("\n답변 요약: ").append(example.getAnswer(), 0, Math.min(example.getAnswer().length(), 200))
                    .append("...\n"));
        }

        return prompt.toString();
    }

    private ToolExecutionResult executeTool(String toolName, Map<String, Object> args) {
        final ZzolBotTool tool = toolsByName.get(toolName);
        if (tool == null) {
            return ToolExecutionResult.fail(toolName, "알 수 없는 tool: " + toolName);
        }
        return tool.execute(args);
    }
}
