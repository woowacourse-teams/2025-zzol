package coffeeshout.friend.application.dto;

import java.time.Instant;

public record FriendRequestPayload(Long requestId, Long fromUserId, String fromUserCode,
                                   String fromNickname, Instant createdAt) {
}
