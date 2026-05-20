package coffeeshout.exception.custom;

import coffeeshout.exception.ErrorCode;

public class InfrastructureException extends CoffeeShoutException {

    public InfrastructureException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public InfrastructureException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
