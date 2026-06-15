package coffeeshout.user.domain;

import coffeeshout.global.exception.custom.BusinessException;

public record UserNickname(String value) {

    public static final int MAX_LENGTH = 10;

    public UserNickname {
        validateNotBlank(value);
        validateLength(value);
    }

    private void validateNotBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(UserErrorCode.NICKNAME_BLANK,
                    "닉네임은 공백일 수 없습니다. 입력값: '" + value + "'");
        }
    }

    private void validateLength(String value) {
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(UserErrorCode.NICKNAME_TOO_LONG,
                    "닉네임은 " + MAX_LENGTH + "자 이하여야 합니다. 현재 길이: " + value.length());
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
