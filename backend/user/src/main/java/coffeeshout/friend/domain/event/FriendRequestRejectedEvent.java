package coffeeshout.friend.domain.event;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestRejectedEvent(
        String eventId,
        Instant timestamp,
        Long requestId,
        Long requesterId,
        Long addresseeId,
        String addresseeUserCode,
        String addresseeNickname
) {

    public FriendRequestRejectedEvent(Long requestId, Long requesterId, Long addresseeId,
                                      String addresseeUserCode, String addresseeNickname) {
        this(UUID.randomUUID().toString(), Instant.now(), requestId, requesterId,
                addresseeId, addresseeUserCode, addresseeNickname);
    }
}
