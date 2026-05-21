package coffeeshout.websocket.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.fixture.IntegrationTestSupport;
import coffeeshout.room.application.service.RoomRecoveryService;
import coffeeshout.websocket.StompSessionManager;
import coffeeshout.websocket.auth.RoomSessionTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class RoomRecoveryControllerTest extends IntegrationTestSupport {

    private static final String TEST_JOIN_CODE = "T3ST";
    private static final String TEST_PLAYER_NAME = "Tester";
    private static final String TEST_SESSION_ID = "session-123";
    private static final String TEST_DESTINATION = "/topic/room/" + TEST_JOIN_CODE;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StompSessionManager stompSessionManager;

    @Autowired
    RoomRecoveryService roomRecoveryService;

    @Autowired
    RoomSessionTokenService roomSessionTokenService;

    @AfterEach
    void tearDown() {
        stompSessionManager.removeSession(TEST_SESSION_ID);
        roomRecoveryService.cleanup(TEST_JOIN_CODE);
    }

    private String issueRoomToken(String joinCode, String playerName) {
        return roomSessionTokenService.issue(joinCode, playerName, null);
    }

    @Test
    void 웹소켓이_연결된_상태에서_복구_요청_시_메시지를_반환한다() throws Exception {
        // given
        stompSessionManager.registerPlayerSession(TEST_JOIN_CODE, TEST_PLAYER_NAME, TEST_SESSION_ID);

        WebSocketResponse<String> response = WebSocketResponse.success("Test Message");
        String savedStreamId = roomRecoveryService.save(TEST_JOIN_CODE, TEST_DESTINATION, response);

        // when & then
        mockMvc.perform(post("/rooms/{joinCode}/recovery", TEST_JOIN_CODE)
                        .header("roomToken", issueRoomToken(TEST_JOIN_CODE, TEST_PLAYER_NAME))
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
        mockMvc.perform(post("/rooms/{joinCode}/recovery", joinCode)
                        .header("roomToken", issueRoomToken(joinCode, playerName))
                        .param("lastId", "0-0"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("웹소켓 미연결"))
                .andExpect(jsonPath("$.messageCount").value(0));
    }

    @Test
    void 유효하지_않은_Stream_ID_형식일_경우_400_에러를_반환한다() throws Exception {
        // given
        stompSessionManager.registerPlayerSession(TEST_JOIN_CODE, TEST_PLAYER_NAME, TEST_SESSION_ID);

        // when & then
        mockMvc.perform(post("/rooms/{joinCode}/recovery", TEST_JOIN_CODE)
                        .header("roomToken", issueRoomToken(TEST_JOIN_CODE, TEST_PLAYER_NAME))
                        .param("lastId", "invalid-stream-id"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roomToken이_없으면_401_에러를_반환한다() throws Exception {
        // when & then
        mockMvc.perform(post("/rooms/{joinCode}/recovery", TEST_JOIN_CODE)
                        .param("lastId", "0-0"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void lastId가_공백일_경우_400_에러를_반환한다(String blankValue) throws Exception {
        // when & then
        mockMvc.perform(post("/rooms/{joinCode}/recovery", TEST_JOIN_CODE)
                        .header("roomToken", issueRoomToken(TEST_JOIN_CODE, TEST_PLAYER_NAME))
                        .param("lastId", blankValue))
                .andExpect(status().isBadRequest());
    }
}
