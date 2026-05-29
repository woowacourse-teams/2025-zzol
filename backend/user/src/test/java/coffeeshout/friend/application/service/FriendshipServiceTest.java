package coffeeshout.friend.application.service;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import coffeeshout.friend.domain.event.FriendRemovedEvent;
import coffeeshout.friend.domain.event.FriendRequestAcceptedEvent;
import coffeeshout.friend.domain.event.FriendRequestCreatedEvent;
import coffeeshout.friend.domain.event.FriendRequestRejectedEvent;

import coffeeshout.fixture.FriendshipFixture;
import coffeeshout.fixture.UserFixture;
import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.friend.domain.FriendErrorCode;
import coffeeshout.UserModuleServiceTest;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FriendshipServiceTest extends UserModuleServiceTest {

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    private User requester;
    private User addressee;

    @BeforeEach
    void setUp() {
        requester = userRepository.save(UserFixture.회원_엠제이());
        addressee = userRepository.save(UserFixture.회원_루키());
    }

    @Nested
    class 친구_요청_보내기 {

        @Test
        void 자기_자신에게_요청하면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> friendshipService.sendRequest(requester.getId(), requester.getId()),
                    FriendErrorCode.CANNOT_FRIEND_SELF
            );
        }

        @Test
        void 이미_친구인_사용자에게_요청하면_예외가_발생한다() {
            final Friendship existing = FriendshipFixture.accepted(requester.getId(), addressee.getId());
            friendshipRepository.save(existing);

            assertCoffeeShoutException(
                    () -> friendshipService.sendRequest(requester.getId(), addressee.getId()),
                    FriendErrorCode.FRIEND_ALREADY_EXISTS
            );
        }

        @Test
        void 이미_대기_중인_요청이_있으면_예외가_발생한다() {
            final Friendship existing = FriendshipFixture.pending(requester.getId(), addressee.getId());
            friendshipRepository.save(existing);

            assertCoffeeShoutException(
                    () -> friendshipService.sendRequest(requester.getId(), addressee.getId()),
                    FriendErrorCode.FRIEND_REQUEST_ALREADY_SENT
            );
        }

        @Test
        void 역방향_PENDING이_있을_때_요청하면_예외가_발생한다() {
            friendshipRepository.save(FriendshipFixture.pending(addressee.getId(), requester.getId()));

            assertCoffeeShoutException(
                    () -> friendshipService.sendRequest(requester.getId(), addressee.getId()),
                    FriendErrorCode.FRIEND_REQUEST_ALREADY_SENT
            );
        }

        @Test
        void 정상_요청시_PENDING_상태로_저장되고_이벤트가_발행된다() {
            final Friendship saved = friendshipService.sendRequest(requester.getId(), addressee.getId());

            assertSoftly(softly -> {
                softly.assertThat(saved.getId()).isNotNull();
                softly.assertThat(saved.getRequesterId()).isEqualTo(requester.getId());
                softly.assertThat(saved.getAddresseeId()).isEqualTo(addressee.getId());
                softly.assertThat(saved.isPending()).isTrue();
            });
            verify(eventPublisher).publishEvent(any(FriendRequestCreatedEvent.class));
        }
    }

    @Nested
    class 친구_요청_수락 {

        @Test
        void 수신자가_아닌_사람이_수락하면_예외가_발생한다() {
            final Friendship pending = friendshipRepository.save(FriendshipFixture.pending(requester.getId(), addressee.getId()));

            assertCoffeeShoutException(
                    () -> friendshipService.accept(requester.getId(), pending.getId()),
                    FriendErrorCode.FRIEND_REQUEST_FORBIDDEN
            );
        }

        @Test
        void 수신자가_수락하면_ACCEPTED_상태로_변경되고_이벤트가_발행된다() {
            final Friendship pending = friendshipRepository.save(FriendshipFixture.pending(requester.getId(), addressee.getId()));

            final Friendship accepted = friendshipService.accept(addressee.getId(), pending.getId());

            assertThat(accepted.isAccepted()).isTrue();
            verify(eventPublisher).publishEvent(any(FriendRequestAcceptedEvent.class));
        }
    }

    @Nested
    class 친구_요청_거절 {

        @Test
        void 수신자가_아닌_사람이_거절하면_예외가_발생한다() {
            final Friendship pending = friendshipRepository.save(FriendshipFixture.pending(requester.getId(), addressee.getId()));

            assertCoffeeShoutException(
                    () -> friendshipService.reject(requester.getId(), pending.getId()),
                    FriendErrorCode.FRIEND_REQUEST_FORBIDDEN
            );
        }

        @Test
        void 수신자가_거절하면_요청이_삭제되고_이벤트가_발행된다() {
            final Friendship pending = friendshipRepository.save(FriendshipFixture.pending(requester.getId(), addressee.getId()));

            friendshipService.reject(addressee.getId(), pending.getId());

            assertThat(friendshipRepository.findById(pending.getId())).isEmpty();
            verify(eventPublisher).publishEvent(any(FriendRequestRejectedEvent.class));
        }
    }

    @Nested
    class 친구_끊기 {

        @Test
        void 친구가_아닌_사용자를_끊으면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> friendshipService.unfriend(requester.getId(), addressee.getId()),
                    FriendErrorCode.NOT_FRIEND
            );
        }

        @Test
        void PENDING_상태에서_친구_끊기를_하면_예외가_발생한다() {
            friendshipRepository.save(FriendshipFixture.pending(requester.getId(), addressee.getId()));

            assertCoffeeShoutException(
                    () -> friendshipService.unfriend(requester.getId(), addressee.getId()),
                    FriendErrorCode.NOT_FRIEND
            );
        }

        @Test
        void 친구_끊기를_하면_관계가_삭제되고_이벤트가_발행된다() {
            friendshipRepository.save(FriendshipFixture.accepted(requester.getId(), addressee.getId()));

            friendshipService.unfriend(requester.getId(), addressee.getId());

            assertThat(friendshipRepository.findBetween(requester.getId(), addressee.getId())).isEmpty();
            verify(eventPublisher).publishEvent(any(FriendRemovedEvent.class));
        }
    }
}
