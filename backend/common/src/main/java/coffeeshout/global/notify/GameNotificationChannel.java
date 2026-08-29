package coffeeshout.global.notify;

/**
 * 인스턴스 경계를 넘는 WS 알림 브로드캐스트 — 수신 인스턴스들이 각자 로컬 클라이언트로 전달한다.
 * 추적 전파는 어댑터 경계가 담당한다(ADR-0021).
 * <p>
 * <b>전제조건 — 발행 측은 이 채널 외의 경로로 같은 알림을 로컬 전달하지 않는다.</b> pub/sub은 발행
 * 인스턴스에도 그대로 배달하므로 채널이 유일한 전달 경로다. 채널에 발행하면서 로컬 전송을 함께
 * 호출하면 발행 인스턴스의 클라이언트만 같은 알림을 두 번 받는다.
 * <p>
 * 중복 전달은 채널의 관심사가 아니다. 복구 저장의 중복은 수신 측 복구 서비스의 Lua 중복 제거가
 * 처리한다 — 메시지 식별자가 페이로드에서 파생되므로 같은 메시지는 어느 인스턴스에서 저장하든
 * 같은 streamId를 돌려받는다.
 */
public interface GameNotificationChannel {

    /**
     * 알림을 채널에 발행한다. 전 인스턴스가 수신해 각자 로컬 클라이언트로 전달한다.
     *
     * @param destination WS destination (예: {@code /topic/room/ABC123/gameState})
     * @param payload     직렬화 가능한 알림 페이로드
     */
    void publish(String destination, Object payload);
}
