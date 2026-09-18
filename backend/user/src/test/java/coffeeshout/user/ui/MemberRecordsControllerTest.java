package coffeeshout.user.ui;

import static coffeeshout.minigame.domain.MiniGameType.BLIND_TIMER;
import static coffeeshout.minigame.domain.MiniGameType.BLOCK_STACKING;
import static coffeeshout.minigame.domain.MiniGameType.RACING_GAME;
import static coffeeshout.minigame.domain.MiniGameType.SPEED_TOUCH;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.UserModuleIntegrationTest;
import coffeeshout.fixture.UserFixture;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery.GameRecord;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery.MiniGameRecords;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery.MostPlayed;
import coffeeshout.gamecommon.MemberRouletteRecordQuery;
import coffeeshout.gamecommon.MemberRouletteRecordQuery.RouletteRecord;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** {@code GET /users/me/records}(#1794). 포트 두 개는 {@code ServiceTestConfig}의 mock이다. */
@AutoConfigureMockMvc
class MemberRecordsControllerTest extends UserModuleIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthTokenService authTokenService;

    @Autowired
    MemberRouletteRecordQuery memberRouletteRecordQuery;

    @Autowired
    MemberMiniGameRecordQuery memberMiniGameRecordQuery;

    private String accessToken;

    @BeforeEach
    void setUp() {
        final User user = userRepository.save(UserFixture.회원_엠제이());
        accessToken = authTokenService.issue(user).accessToken();

        given(memberRouletteRecordQuery.findByUserId(user.getId())).willReturn(new RouletteRecord(3, 5, 14));
        given(memberMiniGameRecordQuery.findByUserId(user.getId()))
                .willReturn(new MiniGameRecords(
                        31,
                        new MostPlayed(RACING_GAME, 12),
                        List.of(
                                new GameRecord(RACING_GAME, 12, 12_340L, 14_020L),
                                new GameRecord(BLOCK_STACKING, 7, 14L, 9L),
                                new GameRecord(BLIND_TIMER, 3, 120L, 410L),
                                new GameRecord(SPEED_TOUCH, 0, null, null))));
    }

    @Test
    void 인증된_사용자는_룰렛_통계와_게임별_기록을_받는다() throws Exception {
        mockMvc.perform(get("/users/me/records").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roulette.winCount").value(3))
                .andExpect(jsonPath("$.roulette.survivalStreak").value(5))
                .andExpect(jsonPath("$.roulette.playCount").value(14))
                .andExpect(jsonPath("$.minigame.totalPlayCount").value(31))
                .andExpect(jsonPath("$.minigame.mostPlayed.type").value("RACING_GAME"))
                .andExpect(jsonPath("$.minigame.mostPlayed.playCount").value(12))
                .andExpect(jsonPath("$.games.length()").value(4))
                .andExpect(jsonPath("$.games[0].type").value("RACING_GAME"))
                .andExpect(jsonPath("$.games[0].playCount").value(12))
                .andExpect(jsonPath("$.games[0].best").value(12_340))
                .andExpect(jsonPath("$.games[0].average").value(14_020))
                .andExpect(jsonPath("$.games[3].type").value("SPEED_TOUCH"))
                .andExpect(jsonPath("$.games[3].playCount").value(0))
                .andExpect(jsonPath("$.games[3].best").isEmpty())
                .andExpect(jsonPath("$.games[3].average").isEmpty())
                .andExpect(content().string(not(containsString("userId"))));
    }

    @Test
    void 토큰_없이_호출하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/users/me/records")).andExpect(status().isUnauthorized());
    }
}
