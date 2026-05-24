package coffeeshout.user.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.fixture.UserFixture;
import coffeeshout.support.IntegrationTestSupport;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.domain.TokenPair;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class UserStatsControllerTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthTokenService authTokenService;

    private String accessToken;

    @BeforeEach
    void setUp() {
        final User user = userRepository.save(UserFixture.회원_엠제이());
        final TokenPair tokens = authTokenService.issue(user);
        accessToken = tokens.accessToken();
    }

    @Nested
    class GET_users_me_stats {

        @Test
        void 인증된_사용자는_초기_통계를_조회할_수_있다() throws Exception {
            mockMvc.perform(get("/users/me/stats")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.winCount").value(0))
                    .andExpect(jsonPath("$.survivalStreak").value(0));
        }

        @Test
        void 토큰_없이_호출하면_401을_반환한다() throws Exception {
            mockMvc.perform(get("/users/me/stats"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class POST_users_me_stats {

        @Test
        void isWinner가_true이면_winCount가_증가하고_survivalStreak이_0이다() throws Exception {
            final String body = objectMapper.writeValueAsString(Map.of("isWinner", true));

            mockMvc.perform(post("/users/me/stats")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.winCount").value(1))
                    .andExpect(jsonPath("$.survivalStreak").value(0));
        }

        @Test
        void isWinner가_false이면_survivalStreak이_증가한다() throws Exception {
            final String body = objectMapper.writeValueAsString(Map.of("isWinner", false));

            mockMvc.perform(post("/users/me/stats")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.winCount").value(0))
                    .andExpect(jsonPath("$.survivalStreak").value(1));
        }

        @Test
        void 토큰_없이_호출하면_401을_반환한다() throws Exception {
            final String body = objectMapper.writeValueAsString(Map.of("isWinner", true));

            mockMvc.perform(post("/users/me/stats")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void isWinner_필드가_없으면_400을_반환한다() throws Exception {
            mockMvc.perform(post("/users/me/stats")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
