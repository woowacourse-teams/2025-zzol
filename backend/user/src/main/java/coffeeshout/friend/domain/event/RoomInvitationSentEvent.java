package coffeeshout.friend.domain.event;

import java.time.Instant;
import java.util.UUID;

public record RoomInvitationSentEvent(
        String eventId,
        Instant timestamp,
        Long inviterUserId,
        String inviterNickname,
        Long targetUserId,
        String joinCode
) {

    public RoomInvitationSentEvent(Long inviterUserId, String inviterNickname, Long targetUserId, String joinCode) {
        this(UUID.randomUUID().toString(), Instant.now(), inviterUserId, inviterNickname, targetUserId, joinCode);
    }
}
