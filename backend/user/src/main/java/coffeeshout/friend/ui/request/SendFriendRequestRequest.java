package coffeeshout.friend.ui.request;

import jakarta.validation.constraints.NotNull;

public record SendFriendRequestRequest(@NotNull Long targetUserId) {
}
