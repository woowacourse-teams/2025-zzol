package coffeeshout.friend.ui.response;

import coffeeshout.friend.domain.Friendship;
import java.time.Instant;

public record SendFriendRequestResponse(
        Long requestId,
        Long targetUserId,
        String status,
        Instant createdAt
) {
    public static SendFriendRequestResponse from(Friendship friendship) {
        return new SendFriendRequestResponse(
                friendship.getId(),
                friendship.getAddresseeId(),
                friendship.getStatus().name(),
                friendship.getCreatedAt()
        );
    }
}
