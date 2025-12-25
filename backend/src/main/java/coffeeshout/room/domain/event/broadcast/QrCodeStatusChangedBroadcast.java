package coffeeshout.room.domain.event.broadcast;

import coffeeshout.room.domain.QrCodeStatus;

public record QrCodeStatusChangedBroadcast(
        String joinCode,
        QrCodeStatus status,
        String qrCodeUrl
) {
}
