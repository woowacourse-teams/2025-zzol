package coffeeshout.global.zzolbot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

    private ZzolBotChatService chatService;
    private List<String> progressLog;
    private Consumer<String> progressCallback;

    @BeforeEach
    void setUp() {
        progressLog = new ArrayList<>();
        progressCallback = progressLog::add;
        chatService = new ZzolBotChatService(
                List.of(),
                llmClient,
                PROPERTIES,
                new ZzolBotPromptTemplate(),
                new PiiMasker()
        );
    }

    @Nested
    class ask_메서드 {

        @Test
        void LLM이_즉시_텍스트를_반환하면_그대로_반환한다() {
            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("방 A4BX는 현재 PLAYING 상태입니다."));

            final String result = chatService.ask("A4BX 방 상태 알려줘", progressCallback);

            assertThat(result).contains("PLAYING");
        }

        @Test
        void 텍스트_응답_시_progressCallback이_호출되지_않는다() {
            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("정상입니다."));

            chatService.ask("질문", progressCallback);

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
                    new PiiMasker()
            );

            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.ToolCallResponse("room_state", Map.of("joinCode", "A4BX")))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("방 A4BX: PLAYING 상태, 플레이어 3명"));

            final String result = chatService.ask("A4BX 방 상태 알려줘", progressCallback);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).contains("PLAYING");
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
                    new PiiMasker()
            );

            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.ToolCallResponse("room_state", Map.of("joinCode", "A4BX")))
                    .willReturn(new ZzolBotLlmResponse.TextResponse("완료"));

            chatService.ask("A4BX 방 상태", progressCallback);

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

            final String result = chatService.ask("알 수 없는 요청", progressCallback);

            assertThat(result).isEqualTo("처리 완료");
        }

        @Test
        void maxLoopIterations_초과_시_안내_메시지를_반환한다() {
            given(llmClient.generate(anyList(), anyList()))
                    .willReturn(new ZzolBotLlmResponse.ToolCallResponse("room_state", Map.of("joinCode", "A4BX")));
            given(llmClient.buildFunctionResponseContent(anyString(), anyString()))
                    .willReturn(Content.fromParts(Part.fromText("tool result")));

            final String result = chatService.ask("복잡한 질문", progressCallback);

            assertThat(result).contains("분석이 복잡하여");
        }
    }
}
