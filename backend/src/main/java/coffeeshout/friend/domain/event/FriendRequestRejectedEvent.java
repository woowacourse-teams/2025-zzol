package coffeeshout.friend.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record FriendRequestRejectedEvent(
        String eventId,
        Instant timestamp,
        Long requestId,
        Long requesterId,
        Long addresseeId
) implements BaseEvent {

    public FriendRequestRejectedEvent(Long requestId, Long requesterId, Long addresseeId) {
        this(UUID.randomUUID().toString(), Instant.now(), requestId, requesterId, addresseeId);
    }
}
