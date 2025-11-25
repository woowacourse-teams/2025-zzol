package coffeeshout.room.domain.player;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.room.domain.RoomErrorCode;

public record PlayerName(String value) {

    private static final int MAX_NAME_LENGTH = 10;

    public PlayerName {
        validatePlayerName(value);
    }

    private void validatePlayerName(String value) {
        validateNotBlank(value);
        validateLength(value);
    }

    private void validateNotBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidArgumentException(RoomErrorCode.PLAYER_NAME_BLANK,
                    "이름은 공백일 수 없습니다. 입력값: '" + value + "'");
        }
    }

    private void validateLength(String value) {
        if (value.length() > MAX_NAME_LENGTH) {
            throw new InvalidArgumentException(RoomErrorCode.PLAYER_NAME_TOO_LONG,
                    "이름은 10자 이하여야 합니다. 현재 길이: " + value.length());
        }
    }
}
