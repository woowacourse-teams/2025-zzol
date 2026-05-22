package coffeeshout.room.infra;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public enum QrCodeErrorCode implements ErrorCode {

    QR_CODE_GENERATION_FAILED(500, "QR 코드 생성에 실패했습니다."),
    QR_CODE_UPLOAD_FAILED(503, "QR 코드 업로드에 실패했습니다."),
    QR_CODE_URL_SIGNING_FAILED(503, "QR 코드 URL 생성에 실패했습니다."),
    ;

    private final int statusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
