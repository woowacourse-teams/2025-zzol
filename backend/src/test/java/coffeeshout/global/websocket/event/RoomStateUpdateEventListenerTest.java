package coffeeshout.global.websocket.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.infra.messaging.RoomEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomStateUpdateEventListenerTest {

    @Mock
    private RoomService roomService;

    @Mock
    private RoomEventPublisher roomEventPublisher;

    @InjectMocks
    private RoomStateUpdateEventListener listener;

    @Test
    void 방이_존재할_때는_상태_브로드캐스트를_수행한다() {
        // given
        final String joinCode = "TEST3";
        final RoomStateUpdateEvent event = new RoomStateUpdateEvent(joinCode, "test reason");

        when(roomService.roomExists(joinCode)).thenReturn(true);

        // when
        listener.handleRoomStateUpdate(event);

        // then
        verify(roomService).roomExists(joinCode);
        verify(roomEventPublisher).publishEvent(any(PlayerListUpdateEvent.class));
    }

    @Test
    void 방이_존재하지_않을_때는_상태_브로드캐스트를_수행하지_않는다() {
        // given
        final String joinCode = "ERROR";
        final RoomStateUpdateEvent event = new RoomStateUpdateEvent(joinCode, "test reason");

        when(roomService.roomExists(joinCode)).thenReturn(false);

        // when
        listener.handleRoomStateUpdate(event);

        // then
        verify(roomService).roomExists(joinCode);
        verify(roomService, never()).getAllPlayers(anyString());
        verify(roomEventPublisher, never()).publishEvent(any());
    }
}
