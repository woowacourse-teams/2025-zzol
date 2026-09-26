package coffeeshout.admin.auth.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.auth.domain.AdminRefreshToken;
import coffeeshout.admin.auth.domain.AdminRefreshTokenRepository;
import coffeeshout.admin.support.AdminApiE2eTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("관리자 인증 API")
class AdminAuthControllerE2eTest extends AdminApiE2eTest {

    private static final String REFRESH = "/admin/api/auth/refresh";
    private static final String LOGOUT = "/admin/api/auth/logout";
    // application-test-base.yml 의 admin.auth.web-origins
    private static final String ADMIN_ORIGIN = "http://localhost:5173";
    // 회원 프론트 자리. CORS 는 통과하지만 관리자 오리진은 아니다. 운영의 www.zzol.site 와 같은 위치다.
    private static final String MEMBER_ORIGIN = "http://localhost:3000";
    // application-test-base.yml 의 admin.auth.emails. 재발급 때 허용목록을 다시 보므로 목록에 있는 계정이어야 한다.
    private static final String ALLOWED = "mj@zzol.site";

    @Autowired
    private AdminRefreshTokenRepository adminRefreshTokenRepository;

    @Nested
    class me {

        @Test
        void 토큰의_이메일을_돌려준다() throws Exception {
            mockMvc.perform(get("/admin/api/auth/me").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(ACTOR));
        }

        @Test
        void 토큰이_없으면_401_이다() throws Exception {
            // 403 이 아니라 401 이어야 화면이 "다시 로그인하면 되는 상황"과 "권한이 없어
            // 소용없는 상황"을 가른다.
            mockMvc.perform(get("/admin/api/auth/me")).andExpect(status().isUnauthorized());
        }

        @Test
        void 망가진_토큰도_401_이다() throws Exception {
            mockMvc.perform(get("/admin/api/auth/me").header("Authorization", "Bearer 아무값"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class login {

        @Test
        void 로그인은_토큰_없이도_열려_있다() throws Exception {
            // 상태 코드로는 가릴 수 없다. 구글 토큰이 가짜라 애플리케이션도 401 을 주고,
            // 보안 체인이 막아도 401 이다. 가르는 것은 <b>본문</b>이다. 체인이 막으면
            // 본문이 비어 있고(HttpStatusEntryPoint), 애플리케이션까지 닿으면 오류 코드가
            // 담긴 ProblemDetail 이 온다.
            final MvcResult result = mockMvc.perform(post("/admin/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"not-a-real-google-token\"}"))
                    .andReturn();

            assertThat(result.getResponse().getContentAsString())
                    .as("로그인 경로가 잠기면 본문 없는 401 이 온다")
                    .contains("errorCode");
        }

        @Test
        void 본문이_비면_400_이다() throws Exception {
            mockMvc.perform(post("/admin/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class refresh {

        @Test
        void refresh_쿠키로_새_액세스_토큰과_회전된_쿠키를_받는다() throws Exception {
            final AdminRefreshToken token = loggedIn();

            final MvcResult result = mockMvc.perform(
                            post(REFRESH).cookie(cookieOf(token)).header("Origin", ADMIN_ORIGIN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andReturn();

            // 같은 family 에 다른 tokenId 가 와야 회전된 것이다.
            assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                    .startsWith(AdminRefreshCookie.NAME + "=" + token.familyId() + ".")
                    .doesNotContain(token.value())
                    .contains("Path=/admin/api/auth", "HttpOnly", "Secure", "SameSite=Strict", "Max-Age=604800");
        }

        @Test
        void 인증_없이_열려_있다() throws Exception {
            // 보안 체인이 막으면 본문 없는 401 이 온다. 애플리케이션까지 닿아야 오류 코드가 담긴다.
            mockMvc.perform(post(REFRESH).header("Origin", ADMIN_ORIGIN))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("ADMIN_REFRESH_TOKEN_INVALID"));
        }

        @Test
        void 허용목록에서_빠진_관리자는_403_이고_이후_재발급도_막힌다() throws Exception {
            final AdminRefreshToken token = loggedInAs("removed@zzol.site");

            mockMvc.perform(post(REFRESH).cookie(cookieOf(token)).header("Origin", ADMIN_ORIGIN))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("NOT_ADMIN"));
            // family 가 폐기돼 다시 목록에 넣어도 이 쿠키로는 못 돌아온다. 구글로 다시 로그인해야 한다.
            assertThat(adminRefreshTokenRepository.rotate(token, token.rotate(), Duration.ofMinutes(5)))
                    .isEmpty();
        }

        @Test
        void 관리자_오리진이_아니면_403_이다() throws Exception {
            // 회원 프론트는 같은 사이트라 SameSite 가 막지 못한다. 오리진 대조가 유일한 방어다.
            final AdminRefreshToken token = loggedIn();

            mockMvc.perform(post(REFRESH).cookie(cookieOf(token)).header("Origin", MEMBER_ORIGIN))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ADMIN_ORIGIN_NOT_ALLOWED"));
        }

        @Test
        void 오리진이_없으면_403_이다() throws Exception {
            mockMvc.perform(post(REFRESH).cookie(cookieOf(loggedIn()))).andExpect(status().isForbidden());
        }
    }

    @Nested
    class logout {

        @Test
        void refresh를_폐기하고_쿠키를_지운다() throws Exception {
            final AdminRefreshToken token = loggedIn();

            final MvcResult result = mockMvc.perform(
                            post(LOGOUT).cookie(cookieOf(token)).header("Origin", ADMIN_ORIGIN))
                    .andExpect(status().isNoContent())
                    .andReturn();

            assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                    .startsWith(AdminRefreshCookie.NAME + "=;")
                    .contains("Max-Age=0", "Path=/admin/api/auth");
            mockMvc.perform(post(REFRESH).cookie(cookieOf(token)).header("Origin", ADMIN_ORIGIN))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 쿠키가_없어도_204_이다() throws Exception {
            mockMvc.perform(post(LOGOUT).header("Origin", ADMIN_ORIGIN)).andExpect(status().isNoContent());
        }

        @Test
        void 관리자_오리진이_아니면_폐기하지_않는다() throws Exception {
            final AdminRefreshToken token = loggedIn();

            mockMvc.perform(post(LOGOUT).cookie(cookieOf(token)).header("Origin", MEMBER_ORIGIN))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(REFRESH).cookie(cookieOf(token)).header("Origin", ADMIN_ORIGIN))
                    .andExpect(status().isOk());
        }
    }

    /** 구글 로그인은 테스트에서 태울 수 없어서, 로그인이 남기는 상태(Redis 의 family)를 직접 만든다. */
    private AdminRefreshToken loggedIn() {
        return loggedInAs(ALLOWED);
    }

    private AdminRefreshToken loggedInAs(String email) {
        final AdminRefreshToken token = AdminRefreshToken.newFamily();
        adminRefreshTokenRepository.save(token, AdminEmail.of(email), Duration.ofMinutes(5));
        return token;
    }

    private static Cookie cookieOf(AdminRefreshToken token) {
        return new Cookie(AdminRefreshCookie.NAME, token.value());
    }
}
