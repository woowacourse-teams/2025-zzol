package coffeeshout.minigame.infra.messaging.consumer;

import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.player.PlayerName;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameSessionInitConsumer implements Consumer<RoomCreateEvent> {

    private final GameSessionService gameSessionService;

    @Override
    public void accept(RoomCreateEvent event) {
        gameSessionService.initSession(new JoinCode(event.joinCode()), new PlayerName(event.hostName()));
    }
}
