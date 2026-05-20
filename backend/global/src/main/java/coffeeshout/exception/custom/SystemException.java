package coffeeshout.exception.custom;

import coffeeshout.exception.ErrorCode;

public class SystemException extends CoffeeShoutException {

    public SystemException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SystemException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
