package coffeeshout.friend.ui.request;

import jakarta.validation.constraints.NotNull;

public record SendRoomInvitationRequest(@NotNull Long targetUserId) {
}
