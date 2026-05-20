package coffeeshout.friend.domain.event;

import coffeeshout.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record RoomInvitationSentEvent(
        String eventId,
        Instant timestamp,
        Long inviterUserId,
        String inviterNickname,
        Long targetUserId,
        String joinCode
) implements BaseEvent {

    public RoomInvitationSentEvent(Long inviterUserId, String inviterNickname, Long targetUserId, String joinCode) {
        this(UUID.randomUUID().toString(), Instant.now(), inviterUserId, inviterNickname, targetUserId, joinCode);
    }
}
