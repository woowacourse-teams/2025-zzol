package coffeeshout.zzolbot.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ZzolBotLlmResponse;
import coffeeshout.zzolbot.domain.ZzolBotMessage;
import com.google.common.collect.ImmutableList;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeminiZzolBotClientTest {

    private static final ZzolBotProperties PROPERTIES = new ZzolBotProperties(
            "test-key",
            "gemini-2.0-flash",
            8,
            new ZzolBotProperties.MonitoringProperties(
                    "http://loki:3100",
                    "http://tempo:3200",
                    "http://prometheus:9090",
                    "local"
            ),
            new ZzolBotProperties.DeterminismProperties(0.1, 0.1),
            60,
            10000L,
            new ZzolBotProperties.SqlProperties(List.of(), 100, 3)
    );

    private static final AskContext CTX = AskContext.stamp("test", List.of(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    @Spy
    private GeminiZzolBotClient geminiZzolBotClient =
            new GeminiZzolBotClient(null, PROPERTIES, new ZzolBotSchemaConverter());

    @Nested
    class generate_메서드 {

        @Test
        void function_call이_없으면_텍스트_응답을_반환한다() {
            final GenerateContentResponse response = mock(GenerateContentResponse.class);
            given(response.functionCalls()).willReturn(ImmutableList.of());
            given(response.text()).willReturn("방 ABC1은 현재 PLAYING 상태입니다.");
            doReturn(response).when(geminiZzolBotClient).callApi(anyList(), any(GenerateContentConfig.class));

            final ZzolBotLlmResponse result = geminiZzolBotClient.generate(
                    List.of(new ZzolBotMessage.UserMessage("ABC1 방 상태 알려줘")),
                    List.of(),
                    "시스템 지시사항",
                    CTX
            );

            assertThat(result).isInstanceOf(ZzolBotLlmResponse.TextResponse.class);
            assertThat(((ZzolBotLlmResponse.TextResponse) result).text()).contains("PLAYING");
        }

        @Test
        void function_call이_있으면_ToolCallsResponse를_반환한다() {
            final FunctionCall functionCall = mock(FunctionCall.class);
            given(functionCall.name()).willReturn(Optional.of("room_state"));
            given(functionCall.args()).willReturn(Optional.of(Map.of("joinCode", "ABC1")));

            final GenerateContentResponse response = mock(GenerateContentResponse.class);
            given(response.functionCalls()).willReturn(ImmutableList.of(functionCall));
            doReturn(response).when(geminiZzolBotClient).callApi(anyList(), any(GenerateContentConfig.class));

            final ZzolBotLlmResponse result = geminiZzolBotClient.generate(
                    List.of(new ZzolBotMessage.UserMessage("ABC1 방 상태")),
                    List.of(),
                    "시스템 지시사항",
                    CTX
            );

            assertThat(result).isInstanceOf(ZzolBotLlmResponse.ToolCallsResponse.class);
            final ZzolBotLlmResponse.ToolCallsResponse toolCalls =
                    (ZzolBotLlmResponse.ToolCallsResponse) result;
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(toolCalls.calls()).hasSize(1);
                softly.assertThat(toolCalls.calls().get(0).toolName()).isEqualTo("room_state");
                softly.assertThat(toolCalls.calls().get(0).args()).containsKey("joinCode");
            });
        }

        @Test
        void callApi_예외_시_RuntimeException으로_전파된다() {
            doThrow(new RuntimeException("Gemini API 호출 실패"))
                    .when(geminiZzolBotClient).callApi(anyList(), any(GenerateContentConfig.class));

            assertThatThrownBy(() -> geminiZzolBotClient.generate(
                    List.of(new ZzolBotMessage.UserMessage("질문")),
                    List.of(),
                    "시스템 지시사항",
                    CTX
            )).isInstanceOf(RuntimeException.class);
        }
    }
}
