package coffeeshout.room.infra.qr;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum QrCodeErrorCode implements ErrorCode {

    QR_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "QR 코드 생성에 실패했습니다."),
    QR_CODE_UPLOAD_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "QR 코드 업로드에 실패했습니다."),
    QR_CODE_URL_SIGNING_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "QR 코드 URL 생성에 실패했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
