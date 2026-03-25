package coffeeshout.global.websocket.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.global.websocket.GameRecoveryService;
import coffeeshout.global.websocket.StompSessionManager;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.support.test.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class GameRecoveryControllerTest {

    private static final String TEST_JOIN_CODE = "T3ST";
    private static final String TEST_PLAYER_NAME = "Tester";
    private static final String TEST_SESSION_ID = "session-123";
    private static final String TEST_DESTINATION = "/topic/room/" + TEST_JOIN_CODE;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StompSessionManager stompSessionManager;

    @Autowired
    GameRecoveryService gameRecoveryService;

    @AfterEach
    void tearDown() {
        stompSessionManager.removeSession(TEST_SESSION_ID);
        gameRecoveryService.cleanup(new JoinCode(TEST_JOIN_CODE));
    }

    @Test
    void 웹소켓이_연결된_상태에서_복구_요청_시_메시지를_반환한다() throws Exception {
        // given
        stompSessionManager.registerPlayerSession(TEST_JOIN_CODE, TEST_PLAYER_NAME, TEST_SESSION_ID);

        WebSocketResponse<String> response = WebSocketResponse.success("Test Message");
        String savedStreamId = gameRecoveryService.save(new JoinCode(TEST_JOIN_CODE), TEST_DESTINATION, response);

        // when & then
        mockMvc.perform(post("/api/rooms/{joinCode}/recovery", TEST_JOIN_CODE)
                        .param("playerName", TEST_PLAYER_NAME)
                        .param("lastId", "0-0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messageCount").value(1))
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages[0].streamId").value(savedStreamId))
                .andExpect(jsonPath("$.messages[0].destination").value(TEST_DESTINATION))
                .andExpect(jsonPath("$.messages[0].response.data").value("Test Message"));
    }

    @Test
    void 웹소켓이_연결되지_않은_상태에서_복구_요청_시_409_에러를_반환한다() throws Exception {
        // given
        String joinCode = "NOTCONNECTED";
        String playerName = "Unknown";

        // when & then
        mockMvc.perform(post("/api/rooms/{joinCode}/recovery", joinCode)
                        .param("playerName", playerName)
                        .param("lastId", "0-0"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("웹소켓 미연결"))
                .andExpect(jsonPath("$.messageCount").value(0));
    }

    @Test
    void 유효하지_않은_Stream_ID_형식일_경우_400_에러를_반환한다() throws Exception {
        // given
        String invalidLastId = "invalid-stream-id";
        stompSessionManager.registerPlayerSession(TEST_JOIN_CODE, TEST_PLAYER_NAME, TEST_SESSION_ID);

        // when & then
        mockMvc.perform(post("/api/rooms/{joinCode}/recovery", TEST_JOIN_CODE)
                        .param("playerName", TEST_PLAYER_NAME)
                        .param("lastId", invalidLastId))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void playerName이_공백일_경우_400_에러를_반환한다(String blankValue) throws Exception {
        // when & then
        mockMvc.perform(post("/api/rooms/{joinCode}/recovery", TEST_JOIN_CODE)
                        .param("playerName", blankValue)
                        .param("lastId", "0-0"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void lastId가_공백일_경우_400_에러를_반환한다(String blankValue) throws Exception {
        // when & then
        mockMvc.perform(post("/api/rooms/{joinCode}/recovery", TEST_JOIN_CODE)
                        .param("playerName", TEST_PLAYER_NAME)
                        .param("lastId", blankValue))
                .andExpect(status().isBadRequest());
    }
}
