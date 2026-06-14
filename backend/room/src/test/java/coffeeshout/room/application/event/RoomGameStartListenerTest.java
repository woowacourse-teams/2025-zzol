package coffeeshout.room.application.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.event.GameSessionStartedEvent;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.domain.Room;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomGameStartListenerTest {

    @Mock
    private RoomQueryService roomQueryService;

    @Mock
    private Room room;

    @InjectMocks
    private RoomGameStartListener listener;

    @Test
    @DisplayName("GameSession 시작 이벤트를 받으면 방을 PLAYING으로 전이한다")
    void 게임_시작_이벤트로_방을_PLAYING_전이한다() {
        // given
        given(roomQueryService.getByJoinCode(new JoinCode("ABCD"))).willReturn(room);

        // when
        listener.handle(new GameSessionStartedEvent("ABCD"));

        // then
        verify(room).markPlaying();
    }
}
