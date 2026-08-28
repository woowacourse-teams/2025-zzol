package coffeeshout.wormgame.infra.messaging;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.docs.WsQueue;
import coffeeshout.websocket.ui.WebSocketResponse;
import coffeeshout.wormgame.application.WormGameService;
import coffeeshout.wormgame.ui.response.WormSnapshotResponse;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * 구독 시점 유니캐스트 스냅샷 — 재접속 즉시 복구의 핵심 경로. 델타 토픽을 구독하는 순간 그 세션에만
 * 풀 스냅샷을 보낸다. 클라는 {@code /user/queue/worm/snapshot}을 델타 토픽보다 먼저 구독해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WormSnapshotSubscriptionHandler {

    static final String DELTA_TOPIC_PATTERN = "/topic/room/{joinCode:.{4}}/worm";
    static final String SNAPSHOT_QUEUE = "/queue/worm/snapshot";

    private final GameSessionService gameSessionService;
    private final WormGameService wormGameService;
    private final LoggingSimpMessagingTemplate messagingTemplate;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    @EventListener
    @WsQueue(
            path = SNAPSHOT_QUEUE,
            payload = WormSnapshotResponse.class,
            description = "델타 토픽(/room/{joinCode}/worm) 구독 시 그 세션에만 보내는 풀 스냅샷 — 재접속 즉시 복구")
    public void handleSubscribe(SessionSubscribeEvent event) {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        final String destination = accessor.getDestination();
        if (destination == null || !pathMatcher.match(DELTA_TOPIC_PATTERN, destination)) {
            return;
        }
        final Principal principal = event.getUser();
        if (principal == null) {
            log.debug("principal 없는 구독 — 스냅샷을 보낼 수 없다: destination={}", destination);
            return;
        }
        final JoinCode joinCode = parseJoinCode(destination);
        if (joinCode == null || gameSessionService.findSession(joinCode).isEmpty()) {
            return; // 형식이 안 맞거나 게임을 쥔 인스턴스가 아니다(배포 창 재접속은 종료 후 복귀로 수용)
        }
        try {
            final WormSnapshotResponse snapshot =
                    WormSnapshotResponse.from(wormGameService.snapshot(joinCode.getValue()));
            messagingTemplate.convertAndSendToUser(
                    principal.getName(), SNAPSHOT_QUEUE, WebSocketResponse.success(snapshot));
        } catch (BusinessException e) {
            log.debug("아직 지렁이 게임이 없어 스냅샷을 건너뜀: joinCode={}", joinCode);
        }
    }

    /** destination은 클라이언트 제어값 — 패턴은 임의 4문자를 통과시키므로 형식 검증 실패는 조용히 무시한다. */
    private JoinCode parseJoinCode(String destination) {
        final String raw = pathMatcher
                .extractUriTemplateVariables(DELTA_TOPIC_PATTERN, destination)
                .get("joinCode");
        try {
            return new JoinCode(raw);
        } catch (BusinessException e) {
            return null;
        }
    }
}
