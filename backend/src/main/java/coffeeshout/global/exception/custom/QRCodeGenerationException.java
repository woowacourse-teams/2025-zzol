package coffeeshout.global.exception.custom;

import coffeeshout.global.exception.ErrorCode;

public class QRCodeGenerationException extends CoffeeShoutException {

    public QRCodeGenerationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
