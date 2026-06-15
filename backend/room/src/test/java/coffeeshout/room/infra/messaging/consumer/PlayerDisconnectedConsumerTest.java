package coffeeshout.room.infra.messaging.consumer;

import static org.mockito.Mockito.verify;

import coffeeshout.room.infra.websocket.DelayedPlayerRemovalService;
import coffeeshout.websocket.event.player.PlayerDisconnectedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerDisconnectedConsumerTest {

    @Mock
    DelayedPlayerRemovalService delayedPlayerRemovalService;

    @InjectMocks
    PlayerDisconnectedConsumer playerDisconnectedConsumer;

    @Test
    void 플레이어_연결_해제_이벤트를_수신하면_지연_삭제가_스케줄링된다() {
        PlayerDisconnectedEvent event = PlayerDisconnectedEvent.create("ABCD:닉네임", "session-1", "SESSION_DISCONNECT");

        playerDisconnectedConsumer.accept(event);

        verify(delayedPlayerRemovalService).schedulePlayerRemoval("ABCD:닉네임", "session-1", "SESSION_DISCONNECT");
    }
}
