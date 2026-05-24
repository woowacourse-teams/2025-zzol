package coffeeshout.room.infra.messaging.consumer;

import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.websocket.PlayerKey;
import coffeeshout.websocket.event.player.PlayerReconnectedEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerReconnectedConsumer implements Consumer<PlayerReconnectedEvent> {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void accept(PlayerReconnectedEvent event) {
        final PlayerKey playerKey = PlayerKey.parse(event.playerKey());
        log.info("플레이어 재연결: joinCode={}, playerName={}", playerKey.joinCode(), playerKey.playerName());
        eventPublisher.publishEvent(new PlayerListUpdateEvent(playerKey.joinCode()));
    }
}
