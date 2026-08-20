package coffeeshout.room.application.event;

import static org.mockito.Mockito.verify;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.event.GameSessionStartedEvent;
import coffeeshout.room.application.port.RoomStatusPort;
import coffeeshout.room.application.service.RoomCommandService;
import coffeeshout.room.domain.RoomState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomGameStartListenerTest {

    @Mock
    private RoomCommandService roomCommandService;

    @Mock
    private RoomStatusPort roomStatusPort;

    @InjectMocks
    private RoomGameStartListener listener;

    /**
     * 전이를 {@code RoomCommandService}가 소유하므로 이 리스너는 위임만 한다. 커맨드 메서드 안에서
     * 전이·저장·친구 알림이 함께 끝나므로, 저장을 빠뜨려 알림만 누락되는 경로가 구조적으로 사라진다(#1266 리뷰).
     */
    @Test
    @DisplayName("GameSession 시작 이벤트를 받으면 인메모리 Room과 영속 RoomEntity를 모두 PLAYING으로 전이한다")
    void 게임_시작_이벤트로_방을_PLAYING_전이한다() {
        // when
        listener.handle(new GameSessionStartedEvent("ABCD"));

        // then
        verify(roomCommandService).markPlaying(new JoinCode("ABCD"));
        verify(roomStatusPort).updateStatus("ABCD", RoomState.PLAYING);
    }
}
