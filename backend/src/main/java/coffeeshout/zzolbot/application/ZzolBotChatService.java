package coffeeshout.zzolbot.application;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.FewShotExample;
import coffeeshout.zzolbot.domain.PiiMasker;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import coffeeshout.zzolbot.domain.ZzolBotChatResult;
import coffeeshout.zzolbot.domain.ZzolBotFeedback;
import coffeeshout.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.zzolbot.domain.ZzolBotMessage;
import coffeeshout.zzolbot.infra.ZzolBotLlmClient;
import coffeeshout.zzolbot.infra.ZzolBotSessionEntity;
import coffeeshout.zzolbot.infra.ZzolBotSessionRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZzolBotChatService {

    private static final int FEEDBACK_POOL_SIZE = 20;

    private final ZzolBotLlmClient llmClient;
    private final ZzolBotProperties properties;
    private final ZzolBotPromptTemplate promptTemplate;
    private final PiiMasker piiMasker;
    private final ZzolBotSessionRepository sessionRepository;
    private final ToolExecutor toolExecutor;
    private final FewShotSelector fewShotSelector;
    private final Clock clock;

    public ZzolBotChatResult ask(String question, String adminUsername, Consumer<String> progressCallback) {
        final List<FewShotExample> pool = sessionRepository.findByFeedbackOrderByCreatedAtDesc(
                        ZzolBotFeedback.GOOD, PageRequest.of(0, FEEDBACK_POOL_SIZE))
                .stream()
                .map(e -> new FewShotExample(e.getId(), e.getQuestion(), e.getAnswer()))
                .toList();
        final FewShotSelector.Selection selection = fewShotSelector.select(question, pool);
        final AskContext ctx = AskContext.stamp(question, selection.ids(), clock);

        final List<ZzolBotMessage> conversation = initConversation(question);
        final String systemInstruction = promptTemplate.build(ctx, selection.examples());

        for (int i = 0; i < properties.maxLoopIterations(); i++) {
            final ZzolBotLlmResponse response = llmClient.generate(
                    conversation, toolExecutor.tools(), systemInstruction, ctx);

            if (response instanceof ZzolBotLlmResponse.TextResponse text) {
                log.debug("[ZzolBot] 최종 응답 완료. iterations={}", i + 1);
                return saveSession(question, text.text(), adminUsername, ctx);
            }

            if (response instanceof ZzolBotLlmResponse.ToolCallsResponse toolCalls) {
                final List<ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem> calls = toolCalls.calls();
                calls.forEach(call -> progressCallback.accept(call.toolName()));
                log.debug("[ZzolBot] tool 병렬 실행. count={}, iteration={}", calls.size(), i + 1);

                final List<ToolExecutionResult> results = toolExecutor.executeAll(calls, ctx);

                for (int j = 0; j < calls.size(); j++) {
                    final ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem call = calls.get(j);
                    final ToolExecutionResult result = results.get(j);
                    conversation.add(new ZzolBotMessage.ToolCallMessage(call.toolName(), call.args()));
                    final String maskedContent = piiMasker.mask(result.content(), ctx.piiSession());
                    conversation.add(new ZzolBotMessage.ToolResultMessage(call.toolName(), maskedContent));
                }
            }
        }

        log.warn("[ZzolBot] maxLoopIterations 초과. question={}", question);
        final String fallback = "분석이 복잡하여 완료하지 못했습니다. 질문을 더 구체적으로 해주세요.";
        return saveSession(question, fallback, adminUsername, ctx);
    }

    @Transactional
    public void applyFeedback(Long sessionId, ZzolBotFeedback feedback) {
        sessionRepository.findById(sessionId).ifPresent(session -> session.applyFeedback(feedback));
    }

    @Transactional(readOnly = true)
    public List<ZzolBotSessionEntity> getRecentSessions() {
        return sessionRepository.findTop20ByOrderByCreatedAtDesc();
    }

    private ZzolBotChatResult saveSession(String question, String answer, String adminUsername, AskContext ctx) {
        final String maskedQuestion = piiMasker.mask(question, ctx.piiSession());
        final String maskedAnswer = piiMasker.mask(answer, ctx.piiSession());
        final ZzolBotSessionEntity session = sessionRepository.save(
                ZzolBotSessionEntity.create(maskedQuestion, maskedAnswer, adminUsername)
        );
        return new ZzolBotChatResult(session.getId(), maskedAnswer);
    }

    private List<ZzolBotMessage> initConversation(String question) {
        final List<ZzolBotMessage> conversation = new ArrayList<>();
        conversation.add(new ZzolBotMessage.AssistantMessage("네, zzol 운영 어시스턴트입니다. 무엇을 도와드릴까요?"));
        conversation.add(new ZzolBotMessage.UserMessage(question));
        return conversation;
    }
}
