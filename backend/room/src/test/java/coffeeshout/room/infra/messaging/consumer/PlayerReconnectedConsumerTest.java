package coffeeshout.room.infra.messaging.consumer;

import static org.mockito.Mockito.verify;

import coffeeshout.room.infra.websocket.DelayedPlayerRemovalService;
import coffeeshout.websocket.event.player.PlayerReconnectedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerReconnectedConsumerTest {

    @Mock
    DelayedPlayerRemovalService delayedPlayerRemovalService;

    @InjectMocks
    PlayerReconnectedConsumer playerReconnectedConsumer;

    @Test
    void 플레이어_재연결_이벤트를_수신하면_스케줄링된_삭제가_취소된다() {
        PlayerReconnectedEvent event = PlayerReconnectedEvent.create("ABCD:닉네임", "session-1");

        playerReconnectedConsumer.accept(event);

        verify(delayedPlayerRemovalService).cancelScheduledRemoval("ABCD:닉네임");
    }
}
