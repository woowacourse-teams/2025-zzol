package coffeeshout.global.websocket.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.global.websocket.GameRecoveryService;
import coffeeshout.global.websocket.StompSessionManager;
import coffeeshout.support.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("GameRecoveryController 통합 테스트")
class GameRecoveryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StompSessionManager stompSessionManager;

    @Autowired
    GameRecoveryService gameRecoveryService;

    @Test
    void 웹소켓이_연결된_상태에서_복구_요청_시_메시지를_반환한다() throws Exception {
        // given
        String joinCode = "T3ST";
        String playerName = "Tester";
        String sessionId = "session-123";
        String destination = "/topic/room/TESTCODE";

        // 1. 세션 등록 (웹소켓 연결 상태 시뮬레이션)
        stompSessionManager.registerPlayerSession(joinCode, playerName, sessionId);

        // 2. 복구할 메시지 저장
        WebSocketResponse<String> response = WebSocketResponse.success("Test Message");
        String messageId = gameRecoveryService.generateMessageId(destination, response);
        
        // Redis Stream에 메시지 저장
        String savedStreamId = gameRecoveryService.save(joinCode, destination, response, messageId);

        // when & then
        mockMvc.perform(post("/api/rooms/{joinCode}/recovery", joinCode)
                        .param("playerName", playerName)
                        .param("lastId", "0-0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messageCount").value(1))
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages[0].streamId").value(savedStreamId))
                .andExpect(jsonPath("$.messages[0].destination").value(destination))
                .andExpect(jsonPath("$.messages[0].response.data").value("Test Message"));
    }

    @Test
    void 웹소켓이_연결되지_않은_상태에서_복구_요청_시_400_에러를_반환한다() throws Exception {
        // given
        String joinCode = "T3ST";
        String playerName = "Unknown";

        // when & then
        mockMvc.perform(post("/api/rooms/{joinCode}/recovery", joinCode)
                        .param("playerName", playerName)
                        .param("lastId", "0-0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("웹소켓 미연결"))
                .andExpect(jsonPath("$.messageCount").value(0));
    }

    @Test
    void 유효하지_않은_파라미터_포함_시_500_에러를_반환한다() throws Exception {
        // given
        // joinCode에 구분자(:)가 포함되면 StompSessionManager에서 예외 발생 -> Controller에서 catch -> 500
        String invalidJoinCode = "CODE:INVALID";
        String playerName = "Tester";

        // when & then
        mockMvc.perform(post("/api/rooms/{joinCode}/recovery", invalidJoinCode)
                        .param("playerName", playerName)
                        .param("lastId", "0-0"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("메시지 복구 실패"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void 필수_파라미터가_공백일_경우_400_에러를_반환한다(String blankValue) throws Exception {
        // given
        String joinCode = "T3ST";

        // when & then (playerName이 공백인 경우)
        mockMvc.perform(post("/api/rooms/{joinCode}/recovery", joinCode)
                        .param("playerName", blankValue)
                        .param("lastId", "0-0"))
                .andExpect(status().isBadRequest());

        // when & then (lastId가 공백인 경우)
        mockMvc.perform(post("/api/rooms/{joinCode}/recovery", joinCode)
                        .param("playerName", "Tester")
                        .param("lastId", blankValue))
                .andExpect(status().isBadRequest());
    }
}
