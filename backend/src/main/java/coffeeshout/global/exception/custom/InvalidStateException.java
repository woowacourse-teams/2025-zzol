package coffeeshout.global.exception.custom;

import coffeeshout.global.exception.ErrorCode;

public class InvalidStateException extends CoffeeShoutException {

    public InvalidStateException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
