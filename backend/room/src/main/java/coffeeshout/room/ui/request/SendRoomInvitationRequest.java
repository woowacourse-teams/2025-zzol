package coffeeshout.room.ui.request;

import jakarta.validation.constraints.NotNull;

public record SendRoomInvitationRequest(@NotNull Long targetUserId) {
}
