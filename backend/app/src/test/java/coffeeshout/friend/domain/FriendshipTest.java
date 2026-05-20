package coffeeshout.friend.domain;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.fixture.FriendshipFixture;
import coffeeshout.friend.exception.FriendErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FriendshipTest {

    @Nested
    class 친구_요청_생성 {

        @Test
        void 자기_자신에게_요청하면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> Friendship.request(1L, 1L),
                    FriendErrorCode.CANNOT_FRIEND_SELF
            );
        }

        @Test
        void 정상_요청시_PENDING_상태로_생성된다() {
            final Friendship friendship = FriendshipFixture.pending(1L, 2L);

            assertSoftly(softly -> {
                softly.assertThat(friendship.getRequesterId()).isEqualTo(1L);
                softly.assertThat(friendship.getAddresseeId()).isEqualTo(2L);
                softly.assertThat(friendship.isPending()).isTrue();
                softly.assertThat(friendship.isAccepted()).isFalse();
            });
        }
    }

    @Nested
    class 친구_요청_수락 {

        @Test
        void 수신자가_아닌_사용자가_수락하면_예외가_발생한다() {
            final Friendship friendship = FriendshipFixture.pending(1L, 2L);

            assertCoffeeShoutException(
                    () -> friendship.acceptBy(1L),
                    FriendErrorCode.FRIEND_REQUEST_FORBIDDEN
            );
        }

        @Test
        void PENDING이_아닌_상태에서_수락하면_예외가_발생한다() {
            final Friendship friendship = FriendshipFixture.accepted(1L, 2L);

            assertCoffeeShoutException(
                    () -> friendship.acceptBy(2L),
                    FriendErrorCode.FRIEND_REQUEST_INVALID_STATE
            );
        }

        @Test
        void 수신자가_수락하면_ACCEPTED_상태로_변경된다() {
            final Friendship friendship = FriendshipFixture.pending(1L, 2L);

            friendship.acceptBy(2L);

            assertThat(friendship.isAccepted()).isTrue();
        }
    }

    @Nested
    class 친구_요청_거절_검증 {

        @Test
        void 수신자가_아닌_사용자가_거절하면_예외가_발생한다() {
            final Friendship friendship = FriendshipFixture.pending(1L, 2L);

            assertCoffeeShoutException(
                    () -> friendship.validateRejectableBy(1L),
                    FriendErrorCode.FRIEND_REQUEST_FORBIDDEN
            );
        }

        @Test
        void PENDING이_아닌_상태에서_거절하면_예외가_발생한다() {
            final Friendship friendship = FriendshipFixture.accepted(1L, 2L);

            assertCoffeeShoutException(
                    () -> friendship.validateRejectableBy(2L),
                    FriendErrorCode.FRIEND_REQUEST_INVALID_STATE
            );
        }

        @Test
        void 수신자가_거절하면_예외가_발생하지_않고_상태가_유지된다() {
            final Friendship friendship = FriendshipFixture.pending(1L, 2L);

            friendship.validateRejectableBy(2L);

            assertThat(friendship.isPending()).isTrue();
        }
    }

    @Nested
    class 관계_참여_여부 {

        @Test
        void 요청자는_관계에_포함된다() {
            final Friendship friendship = FriendshipFixture.pending(1L, 2L);

            assertThat(friendship.involves(1L)).isTrue();
        }

        @Test
        void 수신자는_관계에_포함된다() {
            final Friendship friendship = FriendshipFixture.pending(1L, 2L);

            assertThat(friendship.involves(2L)).isTrue();
        }

        @Test
        void 관계에_없는_사용자는_포함되지_않는다() {
            final Friendship friendship = FriendshipFixture.pending(1L, 2L);

            assertThat(friendship.involves(99L)).isFalse();
        }
    }

    @Nested
    class 상대방_조회 {

        @Test
        void 요청자가_조회하면_수신자_ID를_반환한다() {
            final Friendship friendship = FriendshipFixture.pending(1L, 2L);

            assertThat(friendship.counterpartOf(1L)).isEqualTo(2L);
        }

        @Test
        void 수신자가_조회하면_요청자_ID를_반환한다() {
            final Friendship friendship = FriendshipFixture.pending(1L, 2L);

            assertThat(friendship.counterpartOf(2L)).isEqualTo(1L);
        }
    }
}
