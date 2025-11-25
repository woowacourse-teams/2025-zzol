package coffeeshout.global.exception.custom;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class StorageServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    public StorageServiceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public StorageServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}