package coffeeshout.friend.ui.response;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.UserModuleIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * REST 응답 직렬화가 Boot 3.5(Jackson 2 네이티브) 시점과 바이트 동일함을 잠근다(ADR-0020 Phase 2 검증 항목).
 *
 * <p>기대 JSON은 마이그레이션 직전 커밋(976d76a8, Boot 3.5.3)에서 동일한 오토와이어드 {@code ObjectMapper}로
 * 캡처했다. {@code Instant}가 타임스탬프 숫자로 직렬화되는 것은 Jackson 2 기본값이며,
 * {@code use-jackson2-defaults=true} 호환 모드가 이를 유지하는지가 이 테스트의 핵심 관심사다.
 */
class FriendRequestResponseSerializationTest extends UserModuleIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("FriendRequestResponse 직렬화 결과가 Boot 3.5 기준과 동일하다")
    void 직렬화_결과가_boot_3_5_기준과_동일하다() throws Exception {
        final FriendRequestResponse response = new FriendRequestResponse(
                1L, 2L, "USER1234", "닉네임", Instant.parse("2026-01-01T00:00:00Z")
        );

        final String json = objectMapper.writeValueAsString(response);

        assertThat(json).isEqualTo(
                "{\"requestId\":1,\"userId\":2,\"userCode\":\"USER1234\",\"nickname\":\"닉네임\","
                        + "\"createdAt\":1767225600.000000000}"
        );
    }
}
