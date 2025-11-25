package coffeeshout.room.ui.response;

import coffeeshout.room.domain.QrCodeStatus;
import jakarta.annotation.Nullable;

public record QrCodeStatusResponse(
        QrCodeStatus status,
        @Nullable String qrCodeUrl
) {
    public QrCodeStatusResponse {
        if (status == QrCodeStatus.SUCCESS && qrCodeUrl == null) {
            throw new IllegalArgumentException("SUCCESS 상태에서는 qrCodeUrl이 필수입니다");
        }
        if (status != QrCodeStatus.SUCCESS && qrCodeUrl != null) {
            throw new IllegalArgumentException(status + " 상태에서는 qrCodeUrl이 null이어야 합니다");
        }
    }
}
