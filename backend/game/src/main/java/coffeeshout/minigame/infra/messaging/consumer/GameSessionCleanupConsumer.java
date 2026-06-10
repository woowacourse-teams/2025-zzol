package coffeeshout.minigame.infra.messaging.consumer;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.room.domain.event.RoomRemovedEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 방 삭제 시 GameSession을 정리한다(ADR-0023 결정 6 — 생명주기 자동 관리).
 *
 * <p>GameSession은 인스턴스 로컬 저장소에 있으므로 정리도 생성과 동일한 Stream 경로를 타야
 * 세션을 소유한 인스턴스에 도달한다. {@code deleteSession}은 세션이 없어도 안전하다(멱등).
 */
@Component
@RequiredArgsConstructor
public class GameSessionCleanupConsumer implements Consumer<RoomRemovedEvent> {

    private final GameSessionService gameSessionService;

    @Override
    public void accept(RoomRemovedEvent event) {
        gameSessionService.deleteSession(new JoinCode(event.joinCode()));
    }
}
