package coffeeshout.friend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coffeeshout.friend.application.dto.PresencePayload;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.fixture.FriendshipFixture;
import coffeeshout.fixture.UserFixture;
import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.app.WebSocketIntegrationTestSupport;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * "친구 방에 참여하기" 유저 플로우의 임계 와이어링을 관통한다 —
 * 방 입장(REST) → Redis Stream → Consumer → RoomCommandService → RoomPresencePublisher →
 * PresenceNotifier → 친구의 개인 큐 → 받은 참여 코드로 실제 입장.
 *
 * <p>입장 가능 여부 분기(게임 중·정원참·익명 플레이어)는 {@code RoomMembershipQueryAdapterTest}와
 * {@code RoomPresencePublisherTest}가 소진하므로 여기서 다시 열거하지 않는다(ADR-0033).
 */
@AutoConfigureMockMvc
@DisplayName("친구 방 참여 통합 테스트")
class FriendRoomJoinIntegrationTest extends WebSocketIntegrationTestSupport {

    private static final String PRESENCE_QUEUE = "/user/queue/friends/presence";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    FriendshipRepository friendshipRepository;

    @Autowired
    AuthTokenService authTokenService;

    private String 나의_토큰;
    private String 친구의_토큰;

    @BeforeEach
    void 친구_관계를_맺는다() {
        final User 나 = userRepository.save(UserFixture.회원_엠제이());
        final User 친구 = userRepository.save(UserFixture.회원_루키());
        friendshipRepository.save(FriendshipFixture.accepted(나.getId(), 친구.getId()));

        나의_토큰 = authTokenService.issue(나).accessToken();
        친구의_토큰 = authTokenService.issue(친구).accessToken();
    }

    @Test
    @DisplayName("친구가 방을 만들면 참여 코드를 푸시받아 그 방에 입장할 수 있다")
    void 친구가_방을_만들면_참여_코드를_푸시받아_그_방에_입장할_수_있다() throws Exception {
        try (TestStompSession 나의_세션 = createSessionWithAuthorizationToken(나의_토큰)) {
            final TestStompSession.MessageCollector 수신함 = 나의_세션.subscribe(PRESENCE_QUEUE);

            final String joinCode = 방을_만든다(친구의_토큰);

            final PresencePayload 푸시 = 수신함_에서_방_알림을_기다린다(수신함);
            final SoftAssertions softly = new SoftAssertions();
            softly.assertThat(푸시.joinCode()).isEqualTo(joinCode);
            softly.assertThat(푸시.joinable()).isTrue();
            softly.assertAll();

            방에_입장한다(나의_토큰, 푸시.joinCode());
        }
    }

    private String 방을_만든다(String accessToken) throws Exception {
        final MvcResult 시작 = mockMvc.perform(post("/rooms")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(시작.getResponse().getContentAsString()).get("joinCode").asText();
    }

    private void 방에_입장한다(String accessToken, String joinCode) throws Exception {
        final MvcResult 시작 = mockMvc.perform(post("/rooms/{joinCode}", joinCode)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(시작))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinCode").value(joinCode));
    }

    /**
     * 접속 상태 변경 알림이 먼저 도착할 수 있으므로, 참여 코드가 실린 스냅샷이 나올 때까지 훑는다
     * (위치 기반 get() 금지 — testing-integration.md).
     */
    private PresencePayload 수신함_에서_방_알림을_기다린다(TestStompSession.MessageCollector 수신함) {
        for (int i = 0; i < 10; i++) {
            final MessageResponse response = 수신함.get(5, TimeUnit.SECONDS);
            final PresencePayload payload = payloadAs(response, PresencePayload.class);
            if (payload != null && payload.joinCode() != null) {
                return payload;
            }
        }
        throw new AssertionError("참여 코드가 실린 presence 알림이 도착하지 않았습니다.");
    }
}
