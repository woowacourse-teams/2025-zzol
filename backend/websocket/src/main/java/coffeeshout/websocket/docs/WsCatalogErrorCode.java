package coffeeshout.websocket.docs;

import coffeeshout.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public enum WsCatalogErrorCode implements ErrorCode {

    ANNOTATION_BLANK_PATH(500, "path 가 비어 있습니다."),
    ANNOTATION_INVALID_PATH_FORMAT(500, "path 는 '/' 로 시작해야 합니다."),
    ANNOTATION_VOID_PAYLOAD(500, "payload 가 Void.class 입니다."),
    ANNOTATION_OBJECT_PAYLOAD(500, "payload 에 Object.class 는 허용되지 않습니다."),
    ANNOTATION_BLANK_TOPIC_PATH(500, "respondsOnTopics 에 빈 경로가 포함되어 있습니다."),
    INVALID_ENVELOPE_CLASS(500, "envelope-class 는 record 타입이어야 합니다.");

    private final int httpStatusCode;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
