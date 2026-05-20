package coffeeshout.friend.ui.response;

import coffeeshout.friend.application.PresenceTracker;
import coffeeshout.friend.application.service.FriendWithUser;
import java.time.Instant;

public record FriendResponse(
        Long userId,
        String userCode,
        String nickname,
        Instant since,
        boolean online
) {
    public static FriendResponse from(FriendWithUser friendWithUser, PresenceTracker presenceTracker) {
        return new FriendResponse(
                friendWithUser.friendUser().getId(),
                friendWithUser.friendUser().getUserCode().value(),
                friendWithUser.friendUser().getNickname().value(),
                friendWithUser.since(),
                presenceTracker.isOnline(friendWithUser.friendUser().getId())
        );
    }
}
