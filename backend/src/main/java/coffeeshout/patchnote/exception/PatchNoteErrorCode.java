package coffeeshout.patchnote.exception;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum PatchNoteErrorCode implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "패치노트를 찾을 수 없습니다."),
    INVALID_TITLE(HttpStatus.BAD_REQUEST, "제목은 1~100자여야 합니다."),
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "본문은 비어 있을 수 없습니다."),
    INVALID_CONTENT_LENGTH(HttpStatus.BAD_REQUEST, "본문은 5000자를 초과할 수 없습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "카테고리는 필수입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
