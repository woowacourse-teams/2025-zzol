package coffeeshout.friend.ui.response;

import coffeeshout.friend.application.PresenceTracker;
import coffeeshout.friend.application.service.FriendWithUser;
import coffeeshout.friend.domain.RoomMembership;
import java.time.Instant;

public record FriendResponse(
        Long userId,
        String userCode,
        String nickname,
        Instant since,
        boolean online,
        String joinCode,
        boolean joinable) {
    public static FriendResponse from(
            FriendWithUser friendWithUser, PresenceTracker presenceTracker, RoomMembership membership) {
        final Long friendUserId = friendWithUser.friendUser().getId();
        return new FriendResponse(
                friendUserId,
                friendWithUser.friendUser().getUserCode().value(),
                friendWithUser.friendUser().getNickname().value(),
                friendWithUser.since(),
                presenceTracker.isOnline(friendUserId),
                membership.joinCode(),
                membership.joinable());
    }
}
