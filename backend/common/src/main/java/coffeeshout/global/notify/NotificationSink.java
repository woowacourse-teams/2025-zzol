package coffeeshout.global.notify;

/**
 * 알림 채널이 수신한 메시지를 로컬 클라이언트로 전달하는 지점.
 * <p>
 * 채널 어댑터(:infra)와 전달 구현(:websocket)이 서로를 모른 채 만나도록 :common에 둔다 — 두 모듈
 * 모두 :common만 의존한다. 전달 계층이 없는 인스턴스에는 구현이 없을 수 있고, 그때 채널 어댑터는
 * 구독 자체를 열지 않는다.
 */
public interface NotificationSink {

    /**
     * 수신한 알림을 로컬 클라이언트로 전달한다.
     *
     * @param destination WS destination
     * @param payloadJson 발행 측이 직렬화한 페이로드 JSON 본문
     */
    void deliver(String destination, String payloadJson);
}
