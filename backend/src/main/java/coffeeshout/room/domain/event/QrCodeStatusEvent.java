package coffeeshout.room.domain.event;

import coffeeshout.global.trace.TraceInfo;
import coffeeshout.global.trace.TraceInfoExtractor;
import coffeeshout.global.trace.Traceable;
import coffeeshout.room.domain.QrCodeStatus;
import java.time.Instant;
import java.util.UUID;

public record QrCodeStatusEvent(
        String eventId,
        TraceInfo traceInfo,
        Instant timestamp,
        RoomEventType eventType,
        String joinCode,
        QrCodeStatus status,
        String qrCodeUrl
) implements RoomBaseEvent, Traceable {

    public QrCodeStatusEvent(String joinCode, QrCodeStatus status, String qrCodeUrl) {
        this(
                UUID.randomUUID().toString(),
                TraceInfoExtractor.extract(),
                Instant.now(),
                RoomEventType.QR_CODE_COMPLETE,
                joinCode,
                status,
                status == QrCodeStatus.SUCCESS ? qrCodeUrl : null
        );
    }

    @Override
    public TraceInfo getTraceInfo() {
        return traceInfo;
    }
}
