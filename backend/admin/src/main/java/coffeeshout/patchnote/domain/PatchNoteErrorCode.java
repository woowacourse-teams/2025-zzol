package coffeeshout.patchnote.domain;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public enum PatchNoteErrorCode implements ErrorCode {

    NOT_FOUND(404, "패치노트를 찾을 수 없습니다."),
    INVALID_TITLE(400, "제목은 1~100자여야 합니다."),
    INVALID_CONTENT(400, "본문은 비어 있을 수 없습니다."),
    INVALID_CONTENT_LENGTH(400, "본문은 5000자를 초과할 수 없습니다."),
    INVALID_CATEGORY(400, "카테고리는 필수입니다."),
    ;

    private final int httpStatusCode;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
