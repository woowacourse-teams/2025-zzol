package coffeeshout.report.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.support.app.IntegrationTestSupport;
import coffeeshout.report.ratelimit.ReportRateLimitStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@DisplayName("ReportController 통합 테스트")
class ReportControllerTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ReportRateLimitStore rateLimitStore;

    @BeforeEach
    void setUp() {
        when(rateLimitStore.tryAcquire(any())).thenReturn(true);
    }

    @Nested
    @DisplayName("POST /reports")
    class Submit {

        @Test
        void BUG_신고를_정상_제출하면_201을_반환한다() throws Exception {
            final Map<String, Object> body = Map.of(
                    "category", "BUG",
                    "gameType", "CARD_GAME",
                    "joinCode", "ABC12",
                    "content", "카드게임이 멈춰요."
            );

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }

        @Test
        void BUG_신고에서_gameType과_joinCode가_없어도_201을_반환한다() throws Exception {
            final Map<String, Object> body = Map.of(
                    "category", "BUG",
                    "content", "게임 외 버그입니다."
            );

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }

        @Test
        void SUGGESTION_제출을_정상_처리하면_201을_반환한다() throws Exception {
            final Map<String, Object> body = Map.of(
                    "category", "SUGGESTION",
                    "content", "새 게임을 추가해주세요."
            );

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }

        @Test
        void content가_없으면_400을_반환한다() throws Exception {
            final Map<String, Object> body = Map.of(
                    "category", "SUGGESTION"
            );

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void content가_200자를_초과하면_400을_반환한다() throws Exception {
            final Map<String, Object> body = Map.of(
                    "category", "SUGGESTION",
                    "content", "a".repeat(201)
            );

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void category가_없으면_400을_반환한다() throws Exception {
            final Map<String, Object> body = Map.of(
                    "content", "내용입니다."
            );

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void 유효하지_않은_category_값이면_400을_반환한다() throws Exception {
            final Map<String, Object> body = Map.of(
                    "category", "INVALID",
                    "content", "내용입니다."
            );

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void 레이트_리밋_초과_시_429를_반환한다() throws Exception {
            when(rateLimitStore.tryAcquire(any())).thenReturn(false);

            final Map<String, Object> body = Map.of(
                    "category", "SUGGESTION",
                    "content", "건의합니다."
            );

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isTooManyRequests());
        }
    }
}
