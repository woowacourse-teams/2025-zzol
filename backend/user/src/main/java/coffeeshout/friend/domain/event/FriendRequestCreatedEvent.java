package coffeeshout.friend.domain.event;

import coffeeshout.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record FriendRequestCreatedEvent(
        String eventId,
        Instant timestamp,
        Long requestId,
        Long requesterId,
        String requesterUserCode,
        String requesterNickname,
        Long addresseeId
) implements BaseEvent {

    public FriendRequestCreatedEvent(Long requestId, Long requesterId, String requesterUserCode,
                                     String requesterNickname, Long addresseeId) {
        this(UUID.randomUUID().toString(), Instant.now(), requestId, requesterId,
                requesterUserCode, requesterNickname, addresseeId);
    }
}
