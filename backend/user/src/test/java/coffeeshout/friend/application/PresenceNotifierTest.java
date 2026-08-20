package coffeeshout.friend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;

import coffeeshout.friend.application.dto.PresencePayload;
import coffeeshout.friend.application.port.RoomMembershipQuery;
import coffeeshout.friend.application.service.FriendshipService;
import coffeeshout.friend.domain.RoomMembership;
import coffeeshout.friend.domain.event.PresenceChangedEvent;
import coffeeshout.friend.domain.event.RoomPresenceChangedEvent;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.UserPrincipal;
import coffeeshout.websocket.event.user.UserQueueSubscribedEvent;
import coffeeshout.websocket.ui.WebSocketResponse;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 큐 하나로 전체 스냅샷을 내보낸다는 결정이 지켜지는지 검증한다 —
 * 접속 상태만 바뀌어도 방 정보가 실리고, 방 상태만 바뀌어도 접속 상태가 실려야 한다.
 */
@ExtendWith(MockitoExtension.class)
class PresenceNotifierTest {

    private static final String PRESENCE_QUEUE = "/queue/friends/presence";
    private static final Long 나 = 1L;
    private static final Long 친구_A = 2L;
    private static final Long 친구_B = 3L;

    @Mock
    private FriendshipService friendshipService;

    @Mock
    private LoggingSimpMessagingTemplate messagingTemplate;

    @Mock
    private PresenceTracker presenceTracker;

    @Mock
    private RoomMembershipQuery roomMembershipQuery;

    @InjectMocks
    private PresenceNotifier presenceNotifier;

    private List<PresencePayload> 전송된_페이로드() {
        final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        then(messagingTemplate)
                .should(atLeastOnce())
                .convertAndSendToUser(anyString(), eq(PRESENCE_QUEUE), captor.capture());
        return captor.getAllValues().stream()
                .map(WebSocketResponse.class::cast)
                .map(response -> (PresencePayload) response.data())
                .toList();
    }

    @Nested
    @DisplayName("접속 상태 변경")
    class 접속_상태_변경 {

        @Test
        @DisplayName("접속 알림에도 현재 참여 중인 방 정보를 함께 싣는다")
        void 접속_알림에도_현재_참여_중인_방_정보를_함께_싣는다() {
            given(roomMembershipQuery.findByUserIds(List.of(나)))
                    .willReturn(Map.of(나, new RoomMembership("ABCD", true)));
            given(friendshipService.findAcceptedFriendIds(나)).willReturn(List.of(친구_A));

            presenceNotifier.onPresenceChanged(new PresenceChangedEvent(나, true));

            final PresencePayload payload = 전송된_페이로드().getFirst();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(payload.userId()).isEqualTo(나);
                softly.assertThat(payload.online()).isTrue();
                softly.assertThat(payload.joinCode()).isEqualTo("ABCD");
                softly.assertThat(payload.joinable()).isTrue();
            });
        }

        @Test
        @DisplayName("방에 없으면 참여 코드 없이 내보낸다")
        void 방에_없으면_참여_코드_없이_내보낸다() {
            given(roomMembershipQuery.findByUserIds(List.of(나))).willReturn(Map.of());
            given(friendshipService.findAcceptedFriendIds(나)).willReturn(List.of(친구_A));

            presenceNotifier.onPresenceChanged(new PresenceChangedEvent(나, false));

            final PresencePayload payload = 전송된_페이로드().getFirst();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(payload.joinCode()).isNull();
                softly.assertThat(payload.joinable()).isFalse();
            });
        }

        @Test
        @DisplayName("수락된 친구 전원에게 보낸다")
        void 수락된_친구_전원에게_보낸다() {
            given(roomMembershipQuery.findByUserIds(List.of(나))).willReturn(Map.of());
            given(friendshipService.findAcceptedFriendIds(나)).willReturn(List.of(친구_A, 친구_B));

            presenceNotifier.onPresenceChanged(new PresenceChangedEvent(나, true));

            final ArgumentCaptor<String> principals = ArgumentCaptor.forClass(String.class);
            then(messagingTemplate)
                    .should(atLeastOnce())
                    .convertAndSendToUser(principals.capture(), eq(PRESENCE_QUEUE), any());
            assertThat(principals.getAllValues())
                    .containsExactlyInAnyOrder(UserPrincipal.of(친구_A), UserPrincipal.of(친구_B));
        }
    }

    @Nested
    @DisplayName("방 참여 상태 변경")
    class 방_참여_상태_변경 {

        @Test
        @DisplayName("방 알림에도 현재 접속 상태를 함께 싣는다")
        void 방_알림에도_현재_접속_상태를_함께_싣는다() {
            given(presenceTracker.isOnline(나)).willReturn(true);
            given(friendshipService.findAcceptedFriendIds(나)).willReturn(List.of(친구_A));

            presenceNotifier.onRoomPresenceChanged(new RoomPresenceChangedEvent(나, new RoomMembership("ABCD", false)));

            final PresencePayload payload = 전송된_페이로드().getFirst();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(payload.online()).isTrue();
                softly.assertThat(payload.joinCode()).isEqualTo("ABCD");
                softly.assertThat(payload.joinable()).isFalse();
            });
        }

        @Test
        @DisplayName("방을 떠나면 참여 코드를 비워 내보낸다")
        void 방을_떠나면_참여_코드를_비워_내보낸다() {
            given(presenceTracker.isOnline(나)).willReturn(true);
            given(friendshipService.findAcceptedFriendIds(나)).willReturn(List.of(친구_A));

            presenceNotifier.onRoomPresenceChanged(new RoomPresenceChangedEvent(나, RoomMembership.NONE));

            assertThat(전송된_페이로드().getFirst().joinCode()).isNull();
        }
    }

    @Nested
    @DisplayName("큐 구독 시 초기 푸시")
    class 큐_구독_시_초기_푸시 {

        @Test
        @DisplayName("온라인인 친구만 방 정보와 함께 일괄 전송한다")
        void 온라인인_친구만_방_정보와_함께_일괄_전송한다() {
            given(friendshipService.findAcceptedFriendIds(나)).willReturn(List.of(친구_A, 친구_B));
            given(presenceTracker.isOnline(친구_A)).willReturn(true);
            given(presenceTracker.isOnline(친구_B)).willReturn(false);
            given(roomMembershipQuery.findByUserIds(List.of(친구_A)))
                    .willReturn(Map.of(친구_A, new RoomMembership("ABCD", true)));

            presenceNotifier.onPresenceQueueSubscribe(new UserQueueSubscribedEvent(나, "/user/queue/friends/presence"));

            final List<PresencePayload> payloads = 전송된_페이로드();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(payloads).hasSize(1);
                softly.assertThat(payloads.getFirst().userId()).isEqualTo(친구_A);
                softly.assertThat(payloads.getFirst().joinCode()).isEqualTo("ABCD");
            });
        }

        @Test
        @DisplayName("다른 목적지 구독은 무시한다")
        void 다른_목적지_구독은_무시한다() {
            presenceNotifier.onPresenceQueueSubscribe(new UserQueueSubscribedEvent(나, "/user/queue/friends/requests"));

            then(messagingTemplate).should(never()).convertAndSendToUser(anyString(), anyString(), any());
        }
    }
}
