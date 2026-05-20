package coffeeshout.friend.ui.response;

import coffeeshout.friend.application.service.FriendRequestWithUser;
import java.time.Instant;

public record FriendRequestResponse(
        Long requestId,
        Long userId,
        String userCode,
        String nickname,
        Instant createdAt
) {
    public static FriendRequestResponse from(FriendRequestWithUser req) {
        return new FriendRequestResponse(
                req.requestId(),
                req.counterpart().getId(),
                req.counterpart().getUserCode().value(),
                req.counterpart().getNickname().value(),
                req.createdAt()
        );
    }
}
