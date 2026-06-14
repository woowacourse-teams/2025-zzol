package coffeeshout.friend.domain.event;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestAcceptedEvent(
        String eventId,
        Instant timestamp,
        Long requestId,
        Long requesterId,
        String requesterUserCode,
        String requesterNickname,
        Long addresseeId,
        String addresseeUserCode,
        String addresseeNickname
) {

    public FriendRequestAcceptedEvent(Long requestId,
                                      Long requesterId, String requesterUserCode, String requesterNickname,
                                      Long addresseeId, String addresseeUserCode, String addresseeNickname) {
        this(UUID.randomUUID().toString(), Instant.now(), requestId,
                requesterId, requesterUserCode, requesterNickname,
                addresseeId, addresseeUserCode, addresseeNickname);
    }
}
