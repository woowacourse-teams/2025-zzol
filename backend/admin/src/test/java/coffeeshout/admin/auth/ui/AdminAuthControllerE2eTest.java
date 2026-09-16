package coffeeshout.admin.auth.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.admin.support.AdminApiE2eTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("관리자 인증 API")
class AdminAuthControllerE2eTest extends AdminApiE2eTest {

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
}
