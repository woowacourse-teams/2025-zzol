package coffeeshout.friend.application.service;

import coffeeshout.user.domain.User;
import java.time.Instant;

public record FriendRequestWithUser(Long requestId, User counterpart, Instant createdAt) {
}
