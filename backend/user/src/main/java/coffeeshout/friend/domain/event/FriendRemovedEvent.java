package coffeeshout.friend.domain.event;

import coffeeshout.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record FriendRemovedEvent(
        String eventId,
        Instant timestamp,
        Long removedByUserId,
        Long targetUserId
) implements BaseEvent {

    public FriendRemovedEvent(Long removedByUserId, Long targetUserId) {
        this(UUID.randomUUID().toString(), Instant.now(), removedByUserId, targetUserId);
    }
}
