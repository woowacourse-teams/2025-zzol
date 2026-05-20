package coffeeshout.minigame.infra.event;

import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.event.RoomRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameSessionCleanupListener {

    private final GameSessionService gameSessionService;

    @EventListener
    public void on(RoomRemovedEvent event) {
        gameSessionService.deleteSession(new JoinCode(event.joinCode()));
    }
}
