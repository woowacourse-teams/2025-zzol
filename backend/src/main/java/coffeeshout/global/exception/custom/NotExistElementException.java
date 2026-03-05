package coffeeshout.global.exception.custom;

import coffeeshout.global.exception.ErrorCode;

public class NotExistElementException extends CoffeeShoutException {

    public NotExistElementException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
