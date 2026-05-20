package coffeeshout.zzolbot.infra.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.exception.GlobalErrorCode;
import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.zzolbot.domain.AskContext;
import coffeeshout.zzolbot.domain.ToolExecutionResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.service.RoomQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class RoomStateToolTest {

    private static final AskContext CTX = AskContext.stamp("test", List.of(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    @Mock
    private RoomQueryService roomQueryService;

    @Mock
    private GameSessionService gameSessionService;

    private RoomStateTool roomStateTool;

    @BeforeEach
    void setUp() {
        roomStateTool = new RoomStateTool(roomQueryService, gameSessionService, new ObjectMapper());
    }

    @Nested
    class execute_메서드 {

        @Test
        void 방이_존재하면_방_상태_JSON을_반환한다() {
            final JoinCode joinCode = new JoinCode("A4BX");
            final Room room = RoomFixture.호스트_꾹이(joinCode);
            given(roomQueryService.getByJoinCode(joinCode)).willReturn(room);
            given(gameSessionService.getOrCreateSession(joinCode)).willReturn(new GameSession(joinCode));

            final ToolExecutionResult result = roomStateTool.execute(Map.of("joinCode", "A4BX"), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isTrue();
                softly.assertThat(result.toolName()).isEqualTo(RoomStateTool.TOOL_NAME);
                softly.assertThat(result.content()).contains("A4BX");
                softly.assertThat(result.content()).contains("roomState");
                softly.assertThat(result.content()).contains("players");
            });
        }

        @Test
        void 방이_없으면_실패_결과를_반환한다() {
            given(roomQueryService.getByJoinCode(new JoinCode("A4BX")))
                    .willThrow(new BusinessException(GlobalErrorCode.NOT_EXIST, "방이 존재하지 않습니다."));

            final ToolExecutionResult result = roomStateTool.execute(Map.of("joinCode", "A4BX"), CTX);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.success()).isFalse();
                softly.assertThat(result.toolName()).isEqualTo(RoomStateTool.TOOL_NAME);
                softly.assertThat(result.content()).contains("A4BX");
            });
        }

        @Test
        void 유효하지_않은_joinCode_형식이면_실패_결과를_반환한다() {
            final ToolExecutionResult result = roomStateTool.execute(Map.of("joinCode", "INVALID!!"), CTX);

            assertThat(result.success()).isFalse();
        }

        @Test
        void joinCode_파라미터가_누락되면_실패_결과를_반환한다() {
            final ToolExecutionResult result = roomStateTool.execute(Map.of(), CTX);

            assertThat(result.success()).isFalse();
        }

        @Test
        void joinCode_파라미터가_문자열이_아니면_실패_결과를_반환한다() {
            final ToolExecutionResult result = roomStateTool.execute(Map.of("joinCode", 123), CTX);

            assertThat(result.success()).isFalse();
        }
    }
}
