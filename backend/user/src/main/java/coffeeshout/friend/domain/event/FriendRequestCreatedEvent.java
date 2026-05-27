package coffeeshout.friend.domain.event;

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
) {

    public FriendRequestCreatedEvent(Long requestId, Long requesterId, String requesterUserCode,
                                     String requesterNickname, Long addresseeId) {
        this(UUID.randomUUID().toString(), Instant.now(), requestId, requesterId,
                requesterUserCode, requesterNickname, addresseeId);
    }
}
