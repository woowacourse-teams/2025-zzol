package coffeeshout.exception.custom;

import coffeeshout.exception.ErrorCode;
import lombok.Getter;

@Getter
public abstract class CoffeeShoutException extends RuntimeException {

    private final ErrorCode errorCode;

    protected CoffeeShoutException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected CoffeeShoutException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
