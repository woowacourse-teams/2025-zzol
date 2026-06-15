package coffeeshout.global.redis.stream;

/**
 * Redis Stream 레코드의 필드 이름 정의.
 * <p>
 * 신형 레코드는 MapRecord {payload, traceparent} 구조를 사용한다.
 */
public final class StreamRecordFields {

    /** 이벤트 JSON 본문 */
    public static final String PAYLOAD = "payload";

    /** W3C Trace Context 헤더 (없으면 스팬 없이 처리) */
    public static final String TRACEPARENT = "traceparent";

    /**
     * 구형 ObjectRecord 직렬화 포맷의 단일 필드.
     * <p>
     * 전환 이전에 발행된 메시지를 읽기 위한 폴백으로, 1릴리스 동안 유지 후 제거한다.
     */
    public static final String LEGACY_RAW = "_raw";

    private StreamRecordFields() {
    }
}
