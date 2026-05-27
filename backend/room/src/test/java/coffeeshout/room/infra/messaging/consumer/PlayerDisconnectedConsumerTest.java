package coffeeshout.room.infra.messaging.consumer;

import static org.mockito.Mockito.verify;

import coffeeshout.room.infra.websocket.DelayedPlayerRemovalService;
import coffeeshout.support.ServiceTest;
import coffeeshout.websocket.event.player.PlayerDisconnectedEvent;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PlayerDisconnectedConsumerTest extends ServiceTest {

    @Autowired
    Consumer<PlayerDisconnectedEvent> playerDisconnectedEventConsumer;

    @MockitoBean
    DelayedPlayerRemovalService delayedPlayerRemovalService;

    @Test
    void 플레이어_연결_해제_이벤트를_수신하면_지연_삭제가_스케줄링된다() {
        PlayerDisconnectedEvent event = PlayerDisconnectedEvent.create("ABCD:닉네임", "session-1", "SESSION_DISCONNECT");

        playerDisconnectedEventConsumer.accept(event);

        verify(delayedPlayerRemovalService).schedulePlayerRemoval("ABCD:닉네임", "session-1", "SESSION_DISCONNECT");
    }
}
