package coffeeshout.friend.domain.event;

import java.time.Instant;
import java.util.UUID;

public record PresenceChangedEvent(
        String eventId,
        Instant timestamp,
        Long userId,
        boolean online
) {

    public PresenceChangedEvent(Long userId, boolean online) {
        this(UUID.randomUUID().toString(), Instant.now(), userId, online);
    }
}
