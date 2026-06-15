package coffeeshout.room.domain.event;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.room.domain.QrCodeStatus;
import java.time.Instant;
import java.util.UUID;

public record QrCodeStatusEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        QrCodeStatus status,
        String qrCodeUrl
) implements BaseEvent {

    public QrCodeStatusEvent(String joinCode, QrCodeStatus status, String qrCodeUrl) {
        this(
                UUID.randomUUID().toString(),
                Instant.now(),
                joinCode,
                status,
                status == QrCodeStatus.SUCCESS ? qrCodeUrl : null
        );
    }
}
