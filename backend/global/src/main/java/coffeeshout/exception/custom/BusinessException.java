package coffeeshout.exception.custom;

import coffeeshout.exception.ErrorCode;

public class BusinessException extends CoffeeShoutException {

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
