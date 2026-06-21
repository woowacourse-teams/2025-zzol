package coffeeshout.nunchi.application;

import coffeeshout.nunchi.application.response.NunchiStandResponse;
import coffeeshout.nunchi.application.response.NunchiStateResponse;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.docs.WsTopic;
import coffeeshout.websocket.ui.WebSocketResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 눈치게임 브로드캐스트(ADR-0031 결정 8). 타이밍 값(epoch ms)·stood/collided 리스트는 Flow가 계산해
 * 넘기고, 여기선 컨트랙트 JSON에 맞춰 DTO를 감싸 {@code /topic/...}으로 보낸다.
 */
@Component
@RequiredArgsConstructor
public class NunchiNotifier {

    static final String STAND_DESTINATION_FORMAT = "/topic/room/%s/nunchi/stand";
    static final String STATE_DESTINATION_FORMAT = "/topic/room/%s/nunchi/state";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    @WsTopic(path = "/room/{joinCode}/nunchi/stand", payload = NunchiStandResponse.class,
            description = "눈치게임 일어서기(첫 press 즉시·낙관적) 브로드캐스트 — rank 미포함")
    public void notifyStood(
            String joinCode, String name, int number,
            long serverNowEpochMs, long idleDeadlineEpochMs
    ) {
        send(STAND_DESTINATION_FORMAT, joinCode, WebSocketResponse.success(
                new NunchiStandResponse(name, number, serverNowEpochMs, idleDeadlineEpochMs)));
    }

    @WsTopic(path = "/room/{joinCode}/nunchi/state", payload = NunchiStateResponse.class,
            description = "눈치게임 PLAYING 상태(시작·충돌 후 재개·재접속 스냅샷) 브로드캐스트")
    public void notifyPlaying(
            String joinCode, int currentNumber, List<String> stood,
            long serverNowEpochMs, long idleDeadlineEpochMs, long hardCapEpochMs
    ) {
        send(STATE_DESTINATION_FORMAT, joinCode, WebSocketResponse.success(
                NunchiStateResponse.playing(
                        currentNumber, stood, serverNowEpochMs, idleDeadlineEpochMs, hardCapEpochMs)));
    }

    @WsTopic(path = "/room/{joinCode}/nunchi/state", payload = NunchiStateResponse.class,
            description = "눈치게임 COLLISION_COOLDOWN 상태(충돌 발생) 브로드캐스트")
    public void notifyCollisionCooldown(
            String joinCode, int number, List<String> collided,
            long serverNowEpochMs, long resumeAtEpochMs
    ) {
        send(STATE_DESTINATION_FORMAT, joinCode, WebSocketResponse.success(
                NunchiStateResponse.collisionCooldown(number, collided, serverNowEpochMs, resumeAtEpochMs)));
    }

    @WsTopic(path = "/room/{joinCode}/nunchi/state", payload = NunchiStateResponse.class,
            description = "눈치게임 DONE 상태(종료) 브로드캐스트")
    public void notifyDone(String joinCode) {
        send(STATE_DESTINATION_FORMAT, joinCode, WebSocketResponse.success(NunchiStateResponse.done()));
    }

    private void send(String destinationFormat, String joinCode, Object payload) {
        messagingTemplate.convertAndSend(String.format(destinationFormat, joinCode), payload);
    }
}
