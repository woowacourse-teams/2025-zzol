package coffeeshout.room.domain.player;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ColorErrorCode implements ErrorCode {

    NO_AVAILABLE_COLOR("사용가능한 색깔이 없습니다."),
    INVALID_COLOR_INDEX("색깔 Index가 잘못됐습니다.");

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
