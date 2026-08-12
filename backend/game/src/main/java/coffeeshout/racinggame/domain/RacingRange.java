package coffeeshout.racinggame.domain;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.SystemException;

public record RacingRange(int start, int end) {

    public RacingRange {
        if (start < 0 || end < 0) {
            throw new SystemException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "RacingRange의 start와 end는 0보다 커야합니다.");
        }
        if (start > end) {
            throw new SystemException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "RacingRange의 start는 end보다 작아야 합니다.");
        }
    }
}
