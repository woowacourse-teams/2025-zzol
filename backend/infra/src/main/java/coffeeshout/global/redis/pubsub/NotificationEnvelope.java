package coffeeshout.global.redis.pubsub;

/**
 * 알림 채널이 주고받는 봉투.
 * <p>
 * {@code payload}는 알림 페이로드를 직렬화한 JSON <b>텍스트</b>다. 객체로 담아 봉투째 역직렬화하면
 * 수신 측이 페이로드의 구체 타입을 알아야 하는데, 채널은 타입을 모른 채 실어 나르는 계층이다. 텍스트로
 * 두면 왕복이 바이트 정확해져 수신 인스턴스들이 <b>동일한 문자열</b>을 받는다 — 복구 저장의 메시지
 * 식별자가 페이로드에서 파생되므로(md5) 이 동일성이 인스턴스 간 중복 제거의 전제다.
 *
 * @param destination WS destination
 * @param payload     페이로드 JSON 본문
 * @param traceparent W3C Trace Context 헤더. 발행 시 활성 스팬이 없으면 null (ADR-0021)
 */
public record NotificationEnvelope(String destination, String payload, String traceparent) {}
