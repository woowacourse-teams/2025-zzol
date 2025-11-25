package coffeeshout.global.exception.custom;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidArgumentException extends RuntimeException {

    private final ErrorCode errorCode;

    public InvalidArgumentException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
