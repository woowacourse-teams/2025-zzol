package coffeeshout.global.zzolbot.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ToolExecutionResultTest {

    @Nested
    class ok_팩토리_메서드 {

        @Test
        void 성공_결과를_생성한다() {
            final ToolExecutionResult result = ToolExecutionResult.ok("RoomStateTool", "방 상태 정상");

            assertThat(result)
                    .returns("RoomStateTool", ToolExecutionResult::toolName)
                    .returns("방 상태 정상", ToolExecutionResult::content)
                    .returns(true, ToolExecutionResult::success);
        }
    }

    @Nested
    class fail_팩토리_메서드 {

        @Test
        void 실패_결과를_생성한다() {
            final ToolExecutionResult result = ToolExecutionResult.fail("LokiQueryTool", "Loki 연결 실패");

            assertThat(result)
                    .returns("LokiQueryTool", ToolExecutionResult::toolName)
                    .returns("Loki 연결 실패", ToolExecutionResult::content)
                    .returns(false, ToolExecutionResult::success);
        }
    }
}
