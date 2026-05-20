package coffeeshout.zzolbot.infra.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import coffeeshout.outbox.OutboxEvent;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import coffeeshout.zzolbot.infra.ZzolBotOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxToolTest {

    private static final AskContext CTX = AskContext.stamp("test", List.of(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    @Mock
    private ZzolBotOutboxRepository outboxRepository;

    private OutboxTool outboxTool;

    @BeforeEach
    void setUp() {
        outboxTool = new OutboxTool(outboxRepository, new ObjectMapper());
    }

    @Nested
    class execute_메서드 {

        @Test
        void joinCode_관련_실패_이벤트가_있으면_목록을_반환한다() {
            final OutboxEvent event = OutboxEvent.create("room", "{\"joinCode\":\"A4BX\",\"type\":\"JOIN\"}", "A4BX");
            given(outboxRepository.findByJoinCodeAndStatusInOrderByCreatedAtDesc(
                    eq("A4BX"), anyList(), any()))
                    .willReturn(List.of(event));

            final ToolExecutionResult result = outboxTool.execute(Map.of("joinCode", "A4BX"), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isTrue();
                softly.assertThat(result.toolName()).isEqualTo(OutboxTool.TOOL_NAME);
                softly.assertThat(result.content()).contains("PENDING");
                softly.assertThat(result.content()).contains("room");
            });
        }

        @Test
        void 관련_이벤트가_없으면_빈_배열을_반환한다() {
            given(outboxRepository.findByJoinCodeAndStatusInOrderByCreatedAtDesc(
                    eq("A4BX"), anyList(), any()))
                    .willReturn(List.of());

            final ToolExecutionResult result = outboxTool.execute(Map.of("joinCode", "A4BX"), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isTrue();
                softly.assertThat(result.content()).isEqualTo("[]");
            });
        }
    }
}
