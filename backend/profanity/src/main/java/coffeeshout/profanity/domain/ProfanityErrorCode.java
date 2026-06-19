package coffeeshout.profanity.domain;

import coffeeshout.global.exception.ErrorCode;

public enum ProfanityErrorCode implements ErrorCode {

    WORD_BLANK("P001", "비속어는 공백일 수 없습니다.", 400),
    WORD_TOO_LONG("P002", "비속어는 " + ProfanityWord.MAX_WORD_LENGTH + "자 이하여야 합니다.", 400),
    WORD_NOT_FOUND("P003", "등록되지 않은 비속어입니다.", 404),
    LANGUAGE_REQUIRED("P004", "language는 null일 수 없습니다.", 400),
    SOURCE_REQUIRED("P005", "source는 null일 수 없습니다.", 400),
    WORD_TOO_SHORT("P006", "ASCII 비속어는 " + ProfanityWord.MIN_ASCII_WORD_LENGTH + "자 이상이어야 합니다.", 400);

    private final String code;
    private final String message;
    private final int statusCode;

    ProfanityErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }

    @Override
    public int getStatusCode() { return statusCode; }
}
