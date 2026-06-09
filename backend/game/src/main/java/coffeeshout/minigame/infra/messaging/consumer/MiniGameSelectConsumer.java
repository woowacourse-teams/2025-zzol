package coffeeshout.minigame.infra.messaging.consumer;

import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 미니게임 선택 변경을 GameSession에 반영한다(ADR-0023 결정 4).
 *
 * <p><b>전환기 이중 쓰기.</b> Room을 먼저 갱신한다 — Room이 실제 호스트를 검증(비호스트 거부)하고 선택 변경
 * 알림을 재발행하므로, GameSession을 건드리기 전에 호스트가 보증된다(GameSession 지연 생성의 host가 검증된
 * 호스트로 고정됨). 이어서 검증된 호스트로 GameSession을 미러링한다. Room 대기열·조회 엔드포인트는 Step 5/7에서
 * 제거되며, 그때 RoomService 호출도 함께 걷어낸다.
 */
@Component
@RequiredArgsConstructor
public class MiniGameSelectConsumer implements Consumer<MiniGameSelectEvent> {

    private final RoomService roomService;
    private final GameSessionService gameSessionService;

    @Override
    public void accept(MiniGameSelectEvent event) {
        roomService.updateMiniGames(event);
        gameSessionService.updateGames(event);
    }
}
