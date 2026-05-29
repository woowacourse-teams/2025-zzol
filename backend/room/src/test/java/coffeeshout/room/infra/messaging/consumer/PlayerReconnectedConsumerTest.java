package coffeeshout.room.infra.messaging.consumer;

import static org.mockito.Mockito.verify;

import coffeeshout.room.infra.websocket.DelayedPlayerRemovalService;
import coffeeshout.RoomModuleServiceTest;
import coffeeshout.websocket.event.player.PlayerReconnectedEvent;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class PlayerReconnectedConsumerTest extends RoomModuleServiceTest {

    @Autowired
    Consumer<PlayerReconnectedEvent> playerReconnectedEventConsumer;

    @MockitoSpyBean
    DelayedPlayerRemovalService delayedPlayerRemovalService;

    @Test
    void 플레이어_재연결_이벤트를_수신하면_스케줄링된_삭제가_취소된다() {
        PlayerReconnectedEvent event = PlayerReconnectedEvent.create("ABCD:닉네임", "session-1");

        playerReconnectedEventConsumer.accept(event);

        verify(delayedPlayerRemovalService).cancelScheduledRemoval("ABCD:닉네임");
    }
}
