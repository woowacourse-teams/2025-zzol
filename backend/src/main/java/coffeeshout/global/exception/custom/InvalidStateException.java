package coffeeshout.global.exception.custom;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidStateException extends RuntimeException {

    private final ErrorCode errorCode;

    public InvalidStateException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
