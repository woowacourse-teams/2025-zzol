package coffeeshout.room.domain;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;

@Getter
public final class JoinCode {

    private static final String CHARSET = "ABCDFGHJKLMNPQRSTUVWXYZ346789";
    private static final int CODE_LENGTH = 4;

    private final String value;
    private QrCode qrCode;

    public JoinCode(String value) {
        validate(value);
        this.value = value;
        this.qrCode = QrCode.pending();
    }

    public static JoinCode generate() {
        final List<Integer> asciiCodes = convertAsciiList();
        Collections.shuffle(asciiCodes);
        return new JoinCode(asciiCodes.stream()
                .limit(CODE_LENGTH)
                .map(JoinCode::convertAsciiToString)
                .collect(Collectors.joining()));
    }

    public void assignQrCode(@NonNull QrCode qrCode) {
        this.qrCode = qrCode;
    }

    private void validate(String value) {
        if (value == null) {
            throw new InvalidArgumentException(RoomErrorCode.JOIN_CODE_NULL, "참여 코드는 null일 수 없습니다.");
        }

        validateLength(value);
        validateCharacters(value);
    }

    private void validateLength(String value) {
        if (value.length() != CODE_LENGTH) {
            throw new InvalidArgumentException(RoomErrorCode.JOIN_CODE_ILLEGAL_LENGTH,
                    "4자리 코드여야 합니다. 현재 길이: " + value.length());
        }
    }

    private void validateCharacters(String value) {
        if (value.chars().anyMatch(charCode -> !isValidCharacter(charCode))) {
            throw new InvalidArgumentException(RoomErrorCode.JOIN_CODE_ILLEGAL_CHARACTER,
                    "허용되지 않는 문자가 포함되어 있습니다. 현재 코드: " + value);
        }
    }

    private static List<Integer> convertAsciiList() {
        return JoinCode.CHARSET.chars().boxed().collect(Collectors.toCollection(ArrayList::new));
    }

    private static String convertAsciiToString(int asciiCode) {
        return String.valueOf((char) asciiCode);
    }

    private boolean isValidCharacter(int charCode) {
        return CHARSET.indexOf(charCode) > -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (JoinCode) obj;
        return Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
