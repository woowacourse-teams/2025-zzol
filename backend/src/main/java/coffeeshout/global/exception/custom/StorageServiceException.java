package coffeeshout.global.exception.custom;

import coffeeshout.global.exception.ErrorCode;

public class StorageServiceException extends CoffeeShoutException {

    public StorageServiceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public StorageServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
