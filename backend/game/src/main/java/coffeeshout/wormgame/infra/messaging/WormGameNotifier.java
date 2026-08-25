package coffeeshout.wormgame.infra.messaging;

import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.docs.WsTopic;
import coffeeshout.websocket.ui.WebSocketResponse;
import coffeeshout.wormgame.domain.event.WormGameStateChangedEvent;
import coffeeshout.wormgame.domain.event.WormSnapshotEvent;
import coffeeshout.wormgame.domain.event.WormsMovedEvent;
import coffeeshout.wormgame.ui.response.WormGameStateResponse;
import coffeeshout.wormgame.ui.response.WormSnapshotResponse;
import coffeeshout.wormgame.ui.response.WormsStateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 델타·스냅샷은 복구 저장 없이(transient) 보낸다 — 재접속 복구는 구독 시점 유니캐스트 스냅샷이 맡는다.
 * 상태 전이는 복구 저장을 유지한다(DONE 중 끊긴 클라의 결과 화면 라우팅이 이걸 탄다).
 */
@Component
@RequiredArgsConstructor
public class WormGameNotifier {

    static final String DELTA_DESTINATION = "/topic/room/%s/worm";
    static final String STATE_DESTINATION = "/topic/room/%s/worm/state";
    static final String SNAPSHOT_DESTINATION = "/topic/room/%s/worm/snapshot";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    @EventListener
    @WsTopic(
            path = "/room/{joinCode}/worm",
            payload = WormsStateResponse.class,
            description = "지렁이 게임 틱 델타(20Hz) — 머리 위치·각도·생존·lastSeq. tick 단조증가 아니면 폐기. 복구 저장 제외")
    public void publishDelta(WormsMovedEvent event) {
        messagingTemplate.convertAndSendTransient(
                String.format(DELTA_DESTINATION, event.joinCode()),
                WebSocketResponse.success(WormsStateResponse.from(event)));
    }

    @EventListener
    @WsTopic(
            path = "/room/{joinCode}/worm/state",
            payload = WormGameStateResponse.class,
            description = "지렁이 게임 상태 전이 — DESCRIPTION | PREPARE | PLAYING | FINISH(종료 연출) | DONE")
    public void publishState(WormGameStateChangedEvent event) {
        messagingTemplate.convertAndSend(
                String.format(STATE_DESTINATION, event.joinCode()),
                WebSocketResponse.success(new WormGameStateResponse(event.state())));
    }

    @EventListener
    @WsTopic(
            path = "/room/{joinCode}/worm/snapshot",
            payload = WormSnapshotResponse.class,
            description = "지렁이 게임 주기 풀 스냅샷(정합 검증 보조). 스냅샷 tick 이전 델타 폐기. 복구 저장 제외")
    public void publishSnapshot(WormSnapshotEvent event) {
        messagingTemplate.convertAndSendTransient(
                String.format(SNAPSHOT_DESTINATION, event.joinCode()),
                WebSocketResponse.success(WormSnapshotResponse.from(event)));
    }
}
