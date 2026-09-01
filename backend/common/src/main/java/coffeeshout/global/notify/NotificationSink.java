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
     * @return 로컬 전달에 성공하면 true. false는 이 인스턴스에서 알림이 유실됐다는 뜻이다 — 구현이
     *         실패를 삼키면 호출자(채널 구독자)가 유실을 알 길이 없으므로 반환값으로 드러낸다
     */
    boolean deliver(String destination, String payloadJson);
}
