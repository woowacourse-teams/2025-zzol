package coffeeshout.friend.application.service;

import coffeeshout.user.domain.User;
import java.time.Instant;

public record FriendWithUser(User friendUser, Instant since) {
}
