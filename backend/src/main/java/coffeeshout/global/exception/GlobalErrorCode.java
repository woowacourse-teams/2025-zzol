package coffeeshout.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum GlobalErrorCode implements ErrorCode {

    NOT_EXIST("해당 데이터가 존재하지 않습니다."),
    ;

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
