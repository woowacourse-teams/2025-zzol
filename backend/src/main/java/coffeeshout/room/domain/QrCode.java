package coffeeshout.room.domain;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import lombok.Getter;

@Getter
public final class QrCode {

    private final String url;
    private final QrCodeStatus status;

    private QrCode(String url, QrCodeStatus status) {
        this.url = url;
        this.status = status;
    }

    public static QrCode pending() {
        return new QrCode(null, QrCodeStatus.PENDING);
    }

    public static QrCode success(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidArgumentException(RoomErrorCode.QR_CODE_GENERATION_FAILED,
                    "QR 코드 URL은 공백일 수 없습니다. url: " + url);
        }
        return new QrCode(url, QrCodeStatus.SUCCESS);
    }

    public static QrCode error() {
        return new QrCode(null, QrCodeStatus.ERROR);
    }

    public boolean isPending() {
        return status == QrCodeStatus.PENDING;
    }

    public boolean isSuccess() {
        return status == QrCodeStatus.SUCCESS;
    }

    public boolean isError() {
        return status == QrCodeStatus.ERROR;
    }
}
