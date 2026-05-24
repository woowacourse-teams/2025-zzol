package coffeeshout.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.fixture.UserFixture;
import coffeeshout.support.IntegrationTestSupport;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.domain.TokenPair;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class SecurityConfigTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthTokenService authTokenService;

    @Nested
    class admin_체인 {

        @Test
        void admin_엔드포인트는_비로그인_시_로그인_페이지로_리다이렉트된다() throws Exception {
            final MvcResult result = mockMvc.perform(get("/admin"))
                    .andReturn();

            assertThat(result.getResponse().getStatus())
                    .isIn(HttpStatus.FOUND.value(), HttpStatus.MOVED_PERMANENTLY.value());
        }

        @Test
        void admin_login_페이지는_인증_없이_접근_가능하다() throws Exception {
            mockMvc.perform(get("/admin/login"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class user_체인_permitAll {

        @Test
        void 게임_API는_인증_없이_접근_가능하다() throws Exception {
            mockMvc.perform(get("/rooms/check-joinCode").param("joinCode", "WXYZ"))
                    .andExpect(status().isOk());
        }

        @Test
        void 건의사항_API는_인증_없이_접근_가능하다() throws Exception {
            mockMvc.perform(post("/reports")
                            .contentType("application/json")
                            .content("{\"category\":\"SUGGESTION\",\"content\":\"테스트\"}"))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    class JWT_인증_필터 {

        @Test
        void 유효한_Bearer_토큰이_있으면_users_me가_200을_반환한다() throws Exception {
            final User user = userRepository.save(UserFixture.회원_엠제이());
            final TokenPair tokens = authTokenService.issue(user);

            mockMvc.perform(get("/users/me")
                            .header("Authorization", "Bearer " + tokens.accessToken()))
                    .andExpect(status().isOk());
        }

        @Test
        void 토큰_없이_users_me를_호출하면_401을_반환한다() throws Exception {
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 위변조된_토큰으로_users_me를_호출하면_401을_반환한다() throws Exception {
            mockMvc.perform(get("/users/me")
                            .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
