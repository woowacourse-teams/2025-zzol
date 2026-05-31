package coffeeshout.support;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public final class StubBaseEvent implements BaseEvent {

    private final String id;
    private final Instant timestamp;

    public StubBaseEvent() {
        this(UUID.randomUUID().toString(), Instant.EPOCH);
    }

    public StubBaseEvent(String id, Instant timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    @Override
    public String eventId() { return id; }

    @Override
    public Instant timestamp() { return timestamp; }
}
