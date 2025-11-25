package coffeeshout.global.exception.custom;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class QRCodeGenerationException extends RuntimeException {

    private final ErrorCode errorCode;

    public QRCodeGenerationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
