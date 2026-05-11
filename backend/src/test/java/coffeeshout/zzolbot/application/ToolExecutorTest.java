package coffeeshout.zzolbot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;

import coffeeshout.zzolbot.config.ZzolBotProperties;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import coffeeshout.zzolbot.domain.ZzolBotLlmResponse.ToolCallsResponse.ToolCallItem;
import coffeeshout.zzolbot.domain.ZzolBotTool;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToolExecutorTest {

    private static final ZzolBotProperties PROPERTIES = new ZzolBotProperties(
            "key",
            "gemini-2.0-flash",
            5,
            new ZzolBotProperties.MonitoringProperties("http://loki", "http://tempo", "http://prometheus", "local"),
            new ZzolBotProperties.DeterminismProperties(0.1, 0.1),
            60,
            10000L,
            new ZzolBotProperties.SqlProperties(List.of(), 100, 3)
    );

    private static final AskContext CTX = AskContext.stamp("test", List.of(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    @Mock
    private ZzolBotTool toolA;

    @Mock
    private ZzolBotTool toolB;

    @Mock
    private ZzolBotTool toolC;

    @Nested
    class executeAll_메서드 {

        @Test
        void 도구_3개_호출_시_결과_3개를_반환한다() {
            given(toolA.name()).willReturn("tool_a");
            given(toolB.name()).willReturn("tool_b");
            given(toolC.name()).willReturn("tool_c");
            given(toolA.execute(anyMap(), any())).willReturn(ToolExecutionResult.ok("tool_a", "결과A"));
            given(toolB.execute(anyMap(), any())).willReturn(ToolExecutionResult.ok("tool_b", "결과B"));
            given(toolC.execute(anyMap(), any())).willReturn(ToolExecutionResult.ok("tool_c", "결과C"));

            final ToolExecutor executor = new ToolExecutor(List.of(toolA, toolB, toolC), PROPERTIES);
            final List<ToolCallItem> calls = List.of(
                    new ToolCallItem("tool_a", Map.of()),
                    new ToolCallItem("tool_b", Map.of()),
                    new ToolCallItem("tool_c", Map.of())
            );

            final List<ToolExecutionResult> results = executor.executeAll(calls, CTX);

            assertThat(results).hasSize(3);
        }

        @Test
        void 입력_순서대로_결과를_반환한다() {
            given(toolA.name()).willReturn("tool_a");
            given(toolB.name()).willReturn("tool_b");
            given(toolC.name()).willReturn("tool_c");
            given(toolA.execute(anyMap(), any())).willReturn(ToolExecutionResult.ok("tool_a", "A"));
            given(toolB.execute(anyMap(), any())).willReturn(ToolExecutionResult.ok("tool_b", "B"));
            given(toolC.execute(anyMap(), any())).willReturn(ToolExecutionResult.ok("tool_c", "C"));

            final ToolExecutor executor = new ToolExecutor(List.of(toolA, toolB, toolC), PROPERTIES);
            final List<ToolCallItem> calls = List.of(
                    new ToolCallItem("tool_a", Map.of()),
                    new ToolCallItem("tool_b", Map.of()),
                    new ToolCallItem("tool_c", Map.of())
            );

            final List<ToolExecutionResult> results = executor.executeAll(calls, CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(results.get(0).toolName()).isEqualTo("tool_a");
                softly.assertThat(results.get(1).toolName()).isEqualTo("tool_b");
                softly.assertThat(results.get(2).toolName()).isEqualTo("tool_c");
            });
        }

        @Test
        void 알_수_없는_toolName은_fail_결과로_처리한다() {
            given(toolA.name()).willReturn("tool_a");
            given(toolA.execute(anyMap(), any())).willReturn(ToolExecutionResult.ok("tool_a", "A"));

            final ToolExecutor executor = new ToolExecutor(List.of(toolA), PROPERTIES);
            final List<ToolCallItem> calls = List.of(
                    new ToolCallItem("unknown_tool", Map.of()),
                    new ToolCallItem("tool_a", Map.of())
            );

            final List<ToolExecutionResult> results = executor.executeAll(calls, CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(results.get(0).success()).isFalse();
                softly.assertThat(results.get(0).toolName()).isEqualTo("unknown_tool");
                softly.assertThat(results.get(1).success()).isTrue();
            });
        }

        @Test
        void 도구_하나가_예외를_던져도_나머지는_정상_반환된다() {
            given(toolA.name()).willReturn("tool_a");
            given(toolB.name()).willReturn("tool_b");
            given(toolA.execute(anyMap(), any())).willThrow(new RuntimeException("도구 실패"));
            given(toolB.execute(anyMap(), any())).willReturn(ToolExecutionResult.ok("tool_b", "B"));

            final ToolExecutor executor = new ToolExecutor(List.of(toolA, toolB), PROPERTIES);
            final List<ToolCallItem> calls = List.of(
                    new ToolCallItem("tool_a", Map.of()),
                    new ToolCallItem("tool_b", Map.of())
            );

            final List<ToolExecutionResult> results = executor.executeAll(calls, CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(results.get(0).success()).isFalse();
                softly.assertThat(results.get(1).success()).isTrue();
                softly.assertThat(results.get(1).content()).isEqualTo("B");
            });
        }
    }

    @Nested
    class tools_메서드 {

        @Test
        void 등록된_도구_목록을_반환한다() {
            given(toolA.name()).willReturn("tool_a");
            given(toolB.name()).willReturn("tool_b");

            final ToolExecutor executor = new ToolExecutor(List.of(toolA, toolB), PROPERTIES);

            assertThat(executor.tools()).hasSize(2);
        }

        @Test
        void 도구가_없으면_빈_목록을_반환한다() {
            final ToolExecutor executor = new ToolExecutor(List.of(), PROPERTIES);

            assertThat(executor.tools()).isEmpty();
        }
    }
}
