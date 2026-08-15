package coffeeshout.friend.application.service;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.fixture.FriendshipFixture;
import coffeeshout.fixture.UserFixture;
import coffeeshout.friend.application.PresenceTracker;
import coffeeshout.friend.application.port.RoomInvitationValidator;
import coffeeshout.friend.application.port.RoomMembershipQuery;
import coffeeshout.friend.domain.FriendErrorCode;
import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.RoomMembership;
import coffeeshout.friend.domain.event.RoomInvitationSentEvent;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 초대장은 저장되지 않고 WebSocket 개인 큐로만 전달되므로, 받을 수 없는 상태의 대상에게 보내면
 * 보낸 쪽만 성공으로 알고 초대장은 사라진다. 그 불일치를 서버가 막는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class RoomInvitationServiceTest {

    private static final Long 초대자_ID = 1L;
    private static final Long 대상_ID = 2L;
    private static final String JOIN_CODE = "ABCD";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomInvitationValidator roomInvitationValidator;

    @Mock
    private RoomMembershipQuery roomMembershipQuery;

    @Mock
    private PresenceTracker presenceTracker;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RoomInvitationService roomInvitationService;

    @BeforeEach
    void setUp() {
        final User 초대자 = UserFixture.회원_엠제이();
        final User 대상 = UserFixture.회원_루키();
        given(userRepository.findById(초대자_ID)).willReturn(Optional.of(초대자));
        given(userRepository.findById(대상_ID)).willReturn(Optional.of(대상));

        final Friendship 친구관계 = FriendshipFixture.accepted(초대자_ID, 대상_ID);
        given(friendshipRepository.findBetween(초대자_ID, 대상_ID)).willReturn(Optional.of(친구관계));
    }

    @Nested
    @DisplayName("초대 대상 검증")
    class 초대_대상_검증 {

        @Test
        @DisplayName("접속 중이고 어느 방에도 없으면 초대장을 발행한다")
        void 접속_중이고_어느_방에도_없으면_초대장을_발행한다() {
            given(presenceTracker.isOnline(대상_ID)).willReturn(true);
            given(roomMembershipQuery.findByUserIds(anyCollection())).willReturn(Map.of());

            roomInvitationService.invite(초대자_ID, 대상_ID, JOIN_CODE);

            then(eventPublisher).should().publishEvent(any(RoomInvitationSentEvent.class));
        }

        @Test
        @DisplayName("접속 중이 아닌 친구는 초대할 수 없다")
        void 접속_중이_아닌_친구는_초대할_수_없다() {
            given(presenceTracker.isOnline(대상_ID)).willReturn(false);

            assertCoffeeShoutException(
                    () -> roomInvitationService.invite(초대자_ID, 대상_ID, JOIN_CODE), FriendErrorCode.FRIEND_OFFLINE);
        }

        @Test
        @DisplayName("이미 다른 방에 있는 친구는 초대할 수 없다")
        void 이미_다른_방에_있는_친구는_초대할_수_없다() {
            given(presenceTracker.isOnline(대상_ID)).willReturn(true);
            given(roomMembershipQuery.findByUserIds(anyCollection()))
                    .willReturn(Map.of(대상_ID, new RoomMembership("BCDF", true)));

            assertCoffeeShoutException(
                    () -> roomInvitationService.invite(초대자_ID, 대상_ID, JOIN_CODE),
                    FriendErrorCode.FRIEND_ALREADY_IN_ROOM);
        }

        @Test
        @DisplayName("거절된 초대는 이벤트를 발행하지 않는다")
        void 거절된_초대는_이벤트를_발행하지_않는다() {
            given(presenceTracker.isOnline(대상_ID)).willReturn(false);

            assertCoffeeShoutException(
                    () -> roomInvitationService.invite(초대자_ID, 대상_ID, JOIN_CODE), FriendErrorCode.FRIEND_OFFLINE);

            then(eventPublisher).should(never()).publishEvent(any(Object.class));
        }
    }
}
