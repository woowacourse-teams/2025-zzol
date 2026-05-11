package coffeeshout.global.zzolbot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;

import coffeeshout.global.zzolbot.config.ZzolBotProperties;
import coffeeshout.global.zzolbot.domain.AskContext;
import coffeeshout.global.zzolbot.domain.FewShotExample;
import coffeeshout.global.zzolbot.domain.PiiMasker;
import coffeeshout.global.zzolbot.domain.ToolExecutionResult;
import coffeeshout.global.zzolbot.domain.ZzolBotChatResult;
import coffeeshout.global.zzolbot.domain.ZzolBotFeedback;
import coffeeshout.global.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.global.zzolbot.infra.ZzolBotLlmClient;
import coffeeshout.global.zzolbot.infra.ZzolBotSessionEntity;
import coffeeshout.global.zzolbot.infra.ZzolBotSessionRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZzolBotChatServiceTest {

    private static final ZzolBotProperties PROPERTIES = new ZzolBotProperties(
            "test-key",
            "gemini-2.0-flash",
            5,
            new ZzolBotProperties.MonitoringProperties(
                    "http://loki:3100",
                    "http://tempo:3200",
                    "http://prometheus:9090",
                    "local"
            ),
            new ZzolBotProperties.DeterminismProperties(0.1, 0.1),
            60,
            10000L
    );

    @Mock
    private ZzolBotLlmClient llmClient;

    @Mock
    private ZzolBotSessionRepository sessionRepository;

    @Mock
    private ToolExecutor toolExecutor;

    @Mock
    private FewShotSelector fewShotSelector;

    private ZzolBotChatService chatService;
    private List<String> progressLog;
    private Consumer<String> progressCallback;

    @BeforeEach
    void setUp() {
        progressLog = new ArrayList<>();
        progressCallback = progressLog::add;

        final ZzolBotSessionEntity savedSession = ZzolBotSessionEntity.create("", "", "admin");
        ReflectionTestUtils.setField(savedSession, "id", 1L);
        given(sessionRepository.save(any())).willReturn(savedSession);
        given(sessionRepository.findByFeedbackOrderByCreatedAtDesc(any(), any())).willReturn(List.of());
        given(fewShotSelector.select(any(), any())).willReturn(new FewShotSelector.Selection(List.of(), List.of()));
        given(toolExecutor.tools()).willReturn(List.of());

        chatService = new ZzolBotChatService(
                llmClient,
                PROPERTIES,
                new ZzolBotPromptTemplate(PROPERTIES),
                new PiiMasker(),
                sessionRepository,
                toolExecutor,
                fewShotSelector,
                Clock.systemUTC()
        );
    }

    @Nested
    class ask_메서드 {

        @Test
        void LLM이_즉시_텍스트를_반환하면_답변을_포함한_결과를_반환한다() {
            given(llmClient.generate(anyList(), anyList(), anyString(), any(AskContext.class)))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("방 A4BX는 현재 PLAYING 상태입니다."));

            final ZzolBotChatResult result = chatService.ask("A4BX 방 상태 알려줘", "admin", progressCallback);

            assertThat(result.answer()).contains("PLAYING");
        }

        @Test
        void 세션이_DB에_저장되고_ID를_포함한_결과를_반환한다() {
            given(llmClient.generate(anyList(), anyList(), anyString(), any(AskContext.class)))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("PLAYING 상태입니다."));

            final ZzolBotChatResult result = chatService.ask("A4BX 방 상태", "admin", progressCallback);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.sessionId()).isEqualTo(1L);
                softly.assertThat(result.answer()).isNotBlank();
            });
        }

        @Test
        void 텍스트_응답_시_progressCallback이_호출되지_않는다() {
            given(llmClient.generate(anyList(), anyList(), anyString(), any(AskContext.class)))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("정상입니다."));

            chatService.ask("질문", "admin", progressCallback);

            assertThat(progressLog).isEmpty();
        }

        @Test
        void tool_호출_후_텍스트_응답이_오면_최종_결과를_반환한다() {
            given(toolExecutor.executeAll(anyList(), any(AskContext.class)))
                    .willReturn(List.of(ToolExecutionResult.ok("room_state", "{\"roomState\":\"PLAYING\"}")));

            given(llmClient.generate(anyList(), anyList(), anyString(), any(AskContext.class)))
                    .willReturn(new ZzolBotLlmResponse.ToolCallsResponse(List.of(
                            new ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem("room_state", Map.of("joinCode", "A4BX")))))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("방 A4BX: PLAYING 상태, 플레이어 3명"));

            final ZzolBotChatResult result = chatService.ask("A4BX 방 상태 알려줘", "admin", progressCallback);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.answer()).contains("PLAYING");
                softly.assertThat(progressLog).containsExactly("room_state");
            });
        }

        @Test
        void tool_실행_결과에서_PII가_마스킹된_후_LLM에_전달된다() {
            given(toolExecutor.executeAll(anyList(), any(AskContext.class)))
                    .willReturn(List.of(ToolExecutionResult.ok("room_state", "email=admin@zzol.site, ip=10.0.0.1")));

            given(llmClient.generate(anyList(), anyList(), anyString(), any(AskContext.class)))
                    .willReturn(new ZzolBotLlmResponse.ToolCallsResponse(List.of(
                            new ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem("room_state", Map.of("joinCode", "A4BX")))))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("완료"));

            chatService.ask("A4BX 방 상태", "admin", progressCallback);

            org.mockito.Mockito.verify(llmClient, org.mockito.Mockito.atLeast(2)).generate(
                    argThat(conversation -> conversation.stream()
                            .filter(m -> m instanceof coffeeshout.global.zzolbot.domain.ZzolBotMessage.ToolResultMessage)
                            .map(m -> ((coffeeshout.global.zzolbot.domain.ZzolBotMessage.ToolResultMessage) m).result())
                            .noneMatch(r -> r.contains("admin@zzol.site") || r.contains("10.0.0.1"))
                    ),
                    anyList(),
                    anyString(),
                    any(AskContext.class)
            );
        }

        @Test
        void maxLoopIterations_초과_시_안내_메시지를_반환한다() {
            given(toolExecutor.executeAll(anyList(), any(AskContext.class)))
                    .willReturn(List.of(ToolExecutionResult.fail("room_state", "실패")));
            given(llmClient.generate(anyList(), anyList(), anyString(), any(AskContext.class)))
                    .willReturn(new ZzolBotLlmResponse.ToolCallsResponse(List.of(
                            new ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem("room_state", Map.of("joinCode", "A4BX")))));

            final ZzolBotChatResult result = chatService.ask("복잡한 질문", "admin", progressCallback);

            assertThat(result.answer()).contains("분석이 복잡하여");
        }

        @Test
        void GOOD_피드백_세션이_있으면_systemInstruction에_예시로_주입된다() {
            final FewShotExample goodExample = new FewShotExample(1L, "A4BX 방 상태", "PLAYING 상태입니다.");
            given(fewShotSelector.select(any(), any()))
                    .willReturn(new FewShotSelector.Selection(List.of(goodExample), List.of()));
            given(llmClient.generate(anyList(), anyList(), anyString(), any(AskContext.class)))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("응답"));

            chatService.ask("질문", "admin", progressCallback);

            org.mockito.Mockito.verify(llmClient).generate(
                    anyList(),
                    anyList(),
                    argThat(instruction -> instruction.contains("운영자가 좋은 진단으로 평가한 예시")),
                    any(AskContext.class)
            );
        }
    }

    @Nested
    class applyFeedback_메서드 {

        @Test
        void 세션에_피드백을_적용한다() {
            final ZzolBotSessionEntity session = ZzolBotSessionEntity.create("질문", "답변", "admin");
            given(sessionRepository.findById(1L)).willReturn(Optional.of(session));

            chatService.applyFeedback(1L, ZzolBotFeedback.GOOD);

            assertThat(session.getFeedback()).isEqualTo(ZzolBotFeedback.GOOD);
        }

        @Test
        void 존재하지_않는_세션_ID는_무시한다() {
            given(sessionRepository.findById(999L)).willReturn(Optional.empty());

            chatService.applyFeedback(999L, ZzolBotFeedback.GOOD);
        }
    }

    @Nested
    class getRecentSessions_메서드 {

        @Test
        void 최근_세션_목록을_반환한다() {
            final ZzolBotSessionEntity session = ZzolBotSessionEntity.create("질문", "답변", "admin");
            given(sessionRepository.findTop20ByOrderByCreatedAtDesc()).willReturn(List.of(session));

            final List<ZzolBotSessionEntity> sessions = chatService.getRecentSessions();

            assertThat(sessions).hasSize(1);
        }
    }
}
