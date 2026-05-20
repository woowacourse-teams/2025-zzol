package coffeeshout.minigame.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.fixture.GameSessionFixture;
import coffeeshout.fixture.IntegrationTestSupport;
import coffeeshout.fixture.MiniGameDummy;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@DisplayName("MiniGameRestController 통합 테스트")
class MiniGameRestControllerTest extends IntegrationTestSupport {

    private static final JoinCode JOIN_CODE = new JoinCode("A4BX");
    private static final PlayerName HOST = new PlayerName("꾹이");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    GameSessionRepository gameSessionRepository;

    @Nested
    @DisplayName("전체 미니게임 목록 조회")
    class GetMiniGamesTest {

        @Test
        void 모든_미니게임_타입을_반환한다() throws Exception {
            mockMvc.perform(get("/rooms/minigames"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(MiniGameType.values().length));
        }

        @Test
        void 반환_목록에_모든_타입이_포함된다() throws Exception {
            mockMvc.perform(get("/rooms/minigames"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@=='CARD_GAME')]").exists())
                    .andExpect(jsonPath("$[?(@=='RACING_GAME')]").exists())
                    .andExpect(jsonPath("$[?(@=='LADDER_GAME')]").exists());
        }
    }

    @Nested
    @DisplayName("선택된 미니게임 조회")
    class GetSelectedMiniGamesTest {

        @Test
        void 게임세션이_존재하면_선택된_게임_타입을_반환한다() throws Exception {
            GameSession session = GameSessionFixture.게임세션_게임대기(JOIN_CODE, new MiniGameDummy(), HOST);
            gameSessionRepository.save(session);

            mockMvc.perform(get("/rooms/minigames/selected").param("joinCode", JOIN_CODE.getValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0]").value("CARD_GAME"));
        }

        @Test
        void 게임세션이_없으면_404를_반환한다() throws Exception {
            mockMvc.perform(get("/rooms/minigames/selected").param("joinCode", "XXXX"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("남은 미니게임 조회")
    class GetRemainingMiniGamesTest {

        @Test
        void 게임세션이_존재하면_대기중인_게임_목록을_반환한다() throws Exception {
            GameSession session = GameSessionFixture.게임세션_게임대기(JOIN_CODE, new MiniGameDummy(), HOST);
            gameSessionRepository.save(session);

            mockMvc.perform(get("/rooms/{joinCode}/miniGames/remaining", JOIN_CODE.getValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.remaining.length()").value(1))
                    .andExpect(jsonPath("$.remaining[0]").value("CARD_GAME"));
        }

        @Test
        void 게임세션이_없으면_404를_반환한다() throws Exception {
            mockMvc.perform(get("/rooms/{joinCode}/miniGames/remaining", "XXXX"))
                    .andExpect(status().isNotFound());
        }
    }
}
