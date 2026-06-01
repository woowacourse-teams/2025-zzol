package coffeeshout.websocket.support;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class TestErrorTriggerHandler {

    @MessageMapping("/test/throw-business-error")
    public void throwBusinessError() {
        throw new BusinessException(GlobalErrorCode.NOT_EXIST, GlobalErrorCode.NOT_EXIST.getMessage());
    }

    @MessageMapping("/test/throw-runtime-error")
    public void throwRuntimeError() {
        throw new RuntimeException("예상치 못한 서버 오류");
    }
}
