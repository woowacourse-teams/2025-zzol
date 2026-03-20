package coffeeshout.global.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.time.Instant;

@JsonTypeInfo(use = Id.NAME)
public interface BaseEvent {

    String eventId();

    Instant timestamp();
}
