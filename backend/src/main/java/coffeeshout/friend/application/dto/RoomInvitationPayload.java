package coffeeshout.friend.application.dto;

import coffeeshout.friend.domain.event.RoomInvitationSentEvent;

public record RoomInvitationPayload(Long inviterUserId, String inviterNickname, String joinCode) {

    public static RoomInvitationPayload from(RoomInvitationSentEvent event) {
        return new RoomInvitationPayload(event.inviterUserId(), event.inviterNickname(), event.joinCode());
    }
}
