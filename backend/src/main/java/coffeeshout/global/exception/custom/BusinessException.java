package coffeeshout.global.exception.custom;

import coffeeshout.global.exception.ErrorCode;

public class BusinessException extends CoffeeShoutException {

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
