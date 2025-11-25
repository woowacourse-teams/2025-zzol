package coffeeshout.room.infra.messaging.handler;

import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.room.domain.event.RoomEventType;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.ui.response.WinnerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouletteSpinEventHandler implements RoomEventHandler<RouletteSpinEvent> {

    private final LoggingSimpMessagingTemplate messagingTemplate;
    private final RoulettePersistenceService roulettePersistenceService;

    @Override
    public void handle(RouletteSpinEvent event) {
        try {
            log.info("룰렛 스핀 이벤트 수신: eventId={}, joinCode={}, hostName={}",
                    event.eventId(), event.joinCode(), event.hostName());

            final Winner winner = event.winner();

            // 브로드캐스트 (항상 실행)
            broadcastWinner(event.joinCode(), winner);

            // DB 저장 (락으로 보호 - 중복 저장 방지)
            roulettePersistenceService.saveRouletteResult(event);

            log.info("룰렛 스핀 이벤트 처리 완료: eventId={}, joinCode={}, winner={}",
                    event.eventId(), event.joinCode(), winner.name().value());

        } catch (Exception e) {
            log.error("룰렛 스핀 이벤트 처리 실패", e);
        }
    }

    private void broadcastWinner(String joinCode, Winner winner) {
        final WinnerResponse response = WinnerResponse.from(winner);
        messagingTemplate.convertAndSend("/topic/room/" + joinCode + "/winner",
                WebSocketResponse.success(response));
    }

    @Override
    public RoomEventType getSupportedEventType() {
        return RoomEventType.ROULETTE_SPIN;
    }
}
