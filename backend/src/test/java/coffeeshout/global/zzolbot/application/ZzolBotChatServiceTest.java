package coffeeshout.global.zzolbot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
                    "http://prometheus:9090"
            )
    );

    @Mock
    private ZzolBotLlmClient llmClient;

    @Mock
    private ZzolBotSessionRepository sessionRepository;

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

        chatService = new ZzolBotChatService(
                List.of(),
                llmClient,
                PROPERTIES,
                new ZzolBotPromptTemplate(),
                new PiiMasker(),
                sessionRepository
        );
    }

    @Nested
    class ask_메서드 {

        @Test
        void LLM이_즉시_텍스트를_반환하면_답변을_포함한_결과를_반환한다() {
            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("방 A4BX는 현재 PLAYING 상태입니다."));

            final ZzolBotChatResult result = chatService.ask("A4BX 방 상태 알려줘", "admin", progressCallback);

            assertThat(result.answer()).contains("PLAYING");
        }

        @Test
        void 세션이_DB에_저장되고_ID를_포함한_결과를_반환한다() {
            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("PLAYING 상태입니다."));

            final ZzolBotChatResult result = chatService.ask("A4BX 방 상태", "admin", progressCallback);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.sessionId()).isEqualTo(1L);
                softly.assertThat(result.answer()).isNotBlank();
            });
        }

        @Test
        void 텍스트_응답_시_progressCallback이_호출되지_않는다() {
            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("정상입니다."));

            chatService.ask("질문", "admin", progressCallback);

            assertThat(progressLog).isEmpty();
        }

        @Test
        void tool_호출_후_텍스트_응답이_오면_최종_결과를_반환한다() {
            final ZzolBotTool mockTool = mock(ZzolBotTool.class);
            given(mockTool.name()).willReturn("room_state");
            given(mockTool.execute(anyMap()))
                    .willReturn(ToolExecutionResult.ok("room_state", "{\"roomState\":\"PLAYING\"}"));
            given(llmClient.buildFunctionResponseContent(anyString(), anyString()))
                    .willReturn(Content.fromParts(Part.fromText("tool result")));

            chatService = new ZzolBotChatService(
                    List.of(mockTool),
                    llmClient,
                    PROPERTIES,
                    new ZzolBotPromptTemplate(),
                    new PiiMasker(),
                    sessionRepository
            );

            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.ToolCallResponse("room_state", Map.of("joinCode", "A4BX")))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("방 A4BX: PLAYING 상태, 플레이어 3명"));

            final ZzolBotChatResult result = chatService.ask("A4BX 방 상태 알려줘", "admin", progressCallback);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.answer()).contains("PLAYING");
                softly.assertThat(progressLog).containsExactly("room_state");
            });
        }

        @Test
        void tool_실행_결과에서_PII가_마스킹된_후_LLM에_전달된다() {
            final ZzolBotTool mockTool = mock(ZzolBotTool.class);
            given(mockTool.name()).willReturn("room_state");
            given(mockTool.execute(anyMap()))
                    .willReturn(ToolExecutionResult.ok("room_state", "email=admin@zzol.site, ip=10.0.0.1"));
            given(llmClient.buildFunctionResponseContent(anyString(), anyString()))
                    .willReturn(Content.fromParts(Part.fromText("masked")));

            chatService = new ZzolBotChatService(
                    List.of(mockTool),
                    llmClient,
                    PROPERTIES,
                    new ZzolBotPromptTemplate(),
                    new PiiMasker(),
                    sessionRepository
            );

            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.ToolCallResponse("room_state", Map.of("joinCode", "A4BX")))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("완료"));

            chatService.ask("A4BX 방 상태", "admin", progressCallback);

            verify(llmClient).buildFunctionResponseContent(
                    anyString(),
                    org.mockito.ArgumentMatchers.argThat(content ->
                            !content.contains("admin@zzol.site") && !content.contains("10.0.0.1")
                    )
            );
        }

        @Test
        void 알_수_없는_tool_이름이면_fail_결과로_처리하고_계속_진행한다() {
            given(llmClient.buildFunctionResponseContent(anyString(), anyString()))
                    .willReturn(Content.fromParts(Part.fromText("unknown tool result")));
            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.ToolCallResponse("unknown_tool", Map.of()))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("처리 완료"));

            final ZzolBotChatResult result = chatService.ask("알 수 없는 요청", "admin", progressCallback);

            assertThat(result.answer()).isEqualTo("처리 완료");
        }

        @Test
        void maxLoopIterations_초과_시_안내_메시지를_반환한다() {
            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.ToolCallResponse("room_state", Map.of("joinCode", "A4BX")));
            given(llmClient.buildFunctionResponseContent(anyString(), anyString()))
                    .willReturn(Content.fromParts(Part.fromText("tool result")));

            final ZzolBotChatResult result = chatService.ask("복잡한 질문", "admin", progressCallback);

            assertThat(result.answer()).contains("분석이 복잡하여");
        }

        @Test
        void GOOD_피드백_세션이_있으면_프롬프트에_예시로_주입된다() {
            final ZzolBotSessionEntity goodSession = ZzolBotSessionEntity.create(
                    "A4BX 방 상태", "PLAYING 상태입니다.", "admin"
            );
            given(sessionRepository.findByFeedbackOrderByCreatedAtDesc(eq(ZzolBotFeedback.GOOD), any()))
                    .willReturn(List.of(goodSession));
            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("응답"));

            chatService.ask("질문", "admin", progressCallback);

            verify(llmClient).generate(
                    org.mockito.ArgumentMatchers.argThat(conversation ->
                            conversation.stream().anyMatch(c ->
                                    c.text().contains("운영자가 좋은 진단으로 평가한 예시")
                            )
                    ),
                    anyList()
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
