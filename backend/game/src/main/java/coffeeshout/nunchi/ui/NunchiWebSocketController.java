package coffeeshout.nunchi.ui;

import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.nunchi.domain.event.NunchiCommandEvent;
import coffeeshout.nunchi.infra.NunchiStreamKey;
import coffeeshout.websocket.PlayerKey;
import coffeeshout.websocket.docs.WsReceive;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * 눈치게임 입력 컨트롤러(ADR-0031). press 커맨드만 받아 권위 시각을 찍어 스트림에 발행한다
 * (아키텍처 불변 규칙: Application Service 직접 호출 금지 — {@link StreamPublisher}만 주입).
 * 본문 없는 단순 커맨드라 별도 request DTO를 두지 않는다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class NunchiWebSocketController {

    private final StreamPublisher streamPublisher;

    @MessageMapping("/room/{joinCode}/nunchi/press")
    @WsReceive(
            respondsOnTopics = {
                    "/room/{joinCode}/nunchi/stand",
                    "/room/{joinCode}/nunchi/state"
            },
            description = "눈치게임 press — 서버 수신 권위 시각(Q1)을 찍어 스트림 발행, 컨슈머가 충돌·순위 판정"
    )
    public void press(
            @DestinationVariable String joinCode,
            Principal principal
    ) {
        final String authenticatedPlayerName = PlayerKey.parse(principal.getName()).playerName();
        final NunchiCommandEvent event = NunchiCommandEvent.of(joinCode, authenticatedPlayerName);
        streamPublisher.publish(NunchiStreamKey.INPUT, event);

        log.debug("눈치게임 press 발행: joinCode={}, playerName={}, eventId={}",
                joinCode, authenticatedPlayerName, event.eventId());
    }
}
