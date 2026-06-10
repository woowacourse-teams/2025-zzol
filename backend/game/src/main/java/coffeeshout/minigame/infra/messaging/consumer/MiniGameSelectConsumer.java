package coffeeshout.minigame.infra.messaging.consumer;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 미니게임 선택 변경을 GameSession에 반영한다(ADR-0023 결정 4).
 *
 * <p>호스트 검증은 Room에 위임한다 — GameSession 지연 생성 시 이벤트의 {@code hostName}이 세션 호스트로
 * 고정되므로, 실제 방 호스트인지 먼저 보증해야 한다(Step 6의 {@code GameSessionInitConsumer} 도입 전까지의
 * 생성 경로 보호). 반영 성공 후 in-process 이벤트를 재발행해 선택 목록 브로드캐스트
 * ({@code RoomMessagePublisher.onMiniGameListChanged})를 트리거한다.
 */
@Component
@RequiredArgsConstructor
public class MiniGameSelectConsumer implements Consumer<MiniGameSelectEvent> {

    private final RoomQueryService roomQueryService;
    private final GameSessionService gameSessionService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void accept(MiniGameSelectEvent event) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(event.joinCode()));
        room.validateHost(event.hostName());

        gameSessionService.updateGames(event);
        eventPublisher.publishEvent(event);
    }
}
