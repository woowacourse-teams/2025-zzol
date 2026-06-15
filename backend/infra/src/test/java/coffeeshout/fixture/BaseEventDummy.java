package coffeeshout.fixture;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

public record BaseEventDummy(String eventId, Instant timestamp, String payload) implements BaseEvent {

    public static BaseEventDummy 페이로드(String payload) {
        return new BaseEventDummy(UUID.randomUUID().toString(), Instant.now(), payload);
    }
}
