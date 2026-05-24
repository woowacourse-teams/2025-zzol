package coffeeshout.user.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.fixture.UserFixture;
import coffeeshout.support.IntegrationTestSupport;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.domain.TokenPair;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class UserWithdrawalControllerTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;

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
    class DELETE_users_me {

        @Test
        void 인증된_사용자가_탈퇴하면_204를_반환한다() throws Exception {
            mockMvc.perform(delete("/users/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        void 토큰_없이_호출하면_401을_반환한다() throws Exception {
            mockMvc.perform(delete("/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
