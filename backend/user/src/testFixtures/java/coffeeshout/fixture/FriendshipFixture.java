package coffeeshout.fixture;

import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.FriendshipStatus;
import java.time.Instant;

public class FriendshipFixture {

    public static final Long REQUESTER_ID = 1L;
    public static final Long ADDRESSEE_ID = 2L;

    public static Friendship pending() {
        return Friendship.request(REQUESTER_ID, ADDRESSEE_ID);
    }

    public static Friendship pending(Long requesterId, Long addresseeId) {
        return Friendship.request(requesterId, addresseeId);
    }

    public static Friendship accepted() {
        final Instant now = Instant.now();
        return new Friendship(null, REQUESTER_ID, ADDRESSEE_ID, FriendshipStatus.ACCEPTED, now, now);
    }

    public static Friendship accepted(Long requesterId, Long addresseeId) {
        final Instant now = Instant.now();
        return new Friendship(null, requesterId, addresseeId, FriendshipStatus.ACCEPTED, now, now);
    }
}
