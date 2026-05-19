package coffeeshout.websocket.docs;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum WsCatalogErrorCode implements ErrorCode {

    ANNOTATION_BLANK_PATH(HttpStatus.INTERNAL_SERVER_ERROR, "path 가 비어 있습니다."),
    ANNOTATION_INVALID_PATH_FORMAT(HttpStatus.INTERNAL_SERVER_ERROR, "path 는 '/' 로 시작해야 합니다."),
    ANNOTATION_VOID_PAYLOAD(HttpStatus.INTERNAL_SERVER_ERROR, "payload 가 Void.class 입니다."),
    ANNOTATION_OBJECT_PAYLOAD(HttpStatus.INTERNAL_SERVER_ERROR, "payload 에 Object.class 는 허용되지 않습니다."),
    ANNOTATION_BLANK_TOPIC_PATH(HttpStatus.INTERNAL_SERVER_ERROR, "respondsOnTopics 에 빈 경로가 포함되어 있습니다."),
    INVALID_ENVELOPE_CLASS(HttpStatus.INTERNAL_SERVER_ERROR, "envelope-class 는 record 타입이어야 합니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
