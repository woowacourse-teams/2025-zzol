package coffeeshout.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.fixture.UserFixture;
import coffeeshout.support.app.IntegrationTestSupport;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.domain.TokenPair;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
    class 레거시_백오피스_제거 {

        @Test
        void admin_아래_HTML_경로는_더_이상_없다() throws Exception {
            // Thymeleaf 백오피스를 걷어냈다. 이 경로들이 살아나면 폼 로그인 체인도
            // 같이 돌아왔다는 뜻이고, 그러면 /admin/api/** 를 그쪽이 가로챌 수 있다.
            for (String path : List.of("/admin", "/admin/login", "/admin/profanity", "/admin/zzolbot")) {
                final MvcResult result = mockMvc.perform(get(path)).andReturn();

                assertThat(result.getResponse().getStatus())
                        .as("%s 는 매핑이 없어야 한다", path)
                        .isNotIn(HttpStatus.OK.value(), HttpStatus.FOUND.value());
            }
        }
    }

    @Nested
    class admin_api_체인 {

        @Test
        void 토큰_없이_호출하면_401을_반환한다() throws Exception {
            // 401 이어야 관리자 REST 체인이 잡았다는 뜻이다. 매처 없는 사용자 체인이
            // 먼저 가져가면 응답이 달라진다.
            mockMvc.perform(get("/admin/api/auth/me")).andExpect(status().isUnauthorized());
        }

        @Test
        void 위변조된_토큰으로_호출하면_401을_반환한다() throws Exception {
            mockMvc.perform(get("/admin/api/auth/me").header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 사용자_토큰으로는_관리자_API에_접근할_수_없다() throws Exception {
            // 관리자 secret 이 사용자 JWT secret 으로 폴백될 수 있어, 같은 키로 서명된
            // 사용자 토큰이 관리자로 통과하면 안 된다.
            final User user = userRepository.save(UserFixture.회원_엠제이());
            final TokenPair tokens = authTokenService.issue(user);

            mockMvc.perform(get("/admin/api/auth/me").header("Authorization", "Bearer " + tokens.accessToken()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 로그인_엔드포인트는_인증_없이_접근_가능하다() throws Exception {
            // 인증이 아니라 본문 검증에서 걸려야 한다. 401 이면 공개 경로 설정이 빠진 것이다.
            mockMvc.perform(post("/admin/api/auth/login")
                            .contentType("application/json")
                            .content("{\"idToken\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void dev_login_경로는_어느_프로필에도_없다() throws Exception {
            // 한때 local 프로필에서만 열리는 dev-login 이 있었다. 구글 검증을 건너뛰고
            // 허용목록만 보는 경로라, 프로필을 잘못 띄우면 그대로 인증 우회가 된다.
            // 프로필로 잠그는 대신 경로 자체를 없앴다. 되살아나면 여기서 걸린다.
            mockMvc.perform(post("/admin/api/auth/dev-login")
                            .contentType("application/json")
                            .content("{\"email\":\"mj@zzol.site\"}"))
                    .andExpect(status().isUnauthorized());
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

            mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + tokens.accessToken()))
                    .andExpect(status().isOk());
        }

        @Test
        void 토큰_없이_users_me를_호출하면_401을_반환한다() throws Exception {
            mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
        }

        @Test
        void 위변조된_토큰으로_users_me를_호출하면_401을_반환한다() throws Exception {
            mockMvc.perform(get("/users/me").header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
