package coffeeshout.global.exception.custom;

import coffeeshout.global.exception.ErrorCode;

public class InvalidArgumentException extends CoffeeShoutException {

    public InvalidArgumentException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
