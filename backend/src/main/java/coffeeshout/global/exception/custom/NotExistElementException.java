package coffeeshout.global.exception.custom;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class NotExistElementException extends RuntimeException {

    private final ErrorCode errorCode;

    public NotExistElementException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
