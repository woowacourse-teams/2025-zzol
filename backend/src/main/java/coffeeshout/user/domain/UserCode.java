package coffeeshout.user.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.exception.UserErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record UserCode(String value) {

    static final String CHARSET = "ABCDFGHJKLMNPQRSTUVWXYZ346789";
    static final int CODE_LENGTH = 5;

    public UserCode {
        validate(value);
    }

    public static UserCode generate() {
        final List<Integer> asciiCodes = CHARSET.chars().boxed()
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(asciiCodes);
        final String code = asciiCodes.stream()
                .limit(CODE_LENGTH)
                .map(ascii -> String.valueOf((char) (int) ascii))
                .collect(Collectors.joining());
        return new UserCode(code);
    }

    private void validate(String value) {
        validateNotNull(value);
        validateLength(value);
        validateCharacters(value);
    }

    private void validateNotNull(String value) {
        if (value == null) {
            throw new BusinessException(UserErrorCode.USER_CODE_INVALID, "사용자 코드는 null일 수 없습니다.");
        }
    }

    private void validateLength(String value) {
        if (value.length() != CODE_LENGTH) {
            throw new BusinessException(UserErrorCode.USER_CODE_INVALID,
                    CODE_LENGTH + "자리 코드여야 합니다. 현재 길이: " + value.length());
        }
    }

    private void validateCharacters(String value) {
        if (value.chars().anyMatch(c -> CHARSET.indexOf(c) < 0)) {
            throw new BusinessException(UserErrorCode.USER_CODE_INVALID,
                    "허용되지 않는 문자가 포함되어 있습니다. 현재 코드: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
