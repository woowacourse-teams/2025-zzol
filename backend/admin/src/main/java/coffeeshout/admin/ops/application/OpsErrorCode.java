package coffeeshout.admin.ops.application;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OpsErrorCode implements ErrorCode {
    DEAD_LETTER_NOT_FOUND(404, "존재하지 않는 격리 메시지입니다."),
    // 다시 넣기와 폐기가 같이 쓴다. 메시지에 동작 이름을 넣지 않는 이유다.
    NOT_DEAD_LETTER(409, "격리 상태가 아닌 메시지입니다.");

    private final int statusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
