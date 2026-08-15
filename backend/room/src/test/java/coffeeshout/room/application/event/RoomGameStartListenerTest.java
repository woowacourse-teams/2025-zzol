package coffeeshout.room.application.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.event.GameSessionStartedEvent;
import coffeeshout.room.application.port.RoomStatusPort;
import coffeeshout.room.application.service.RoomCommandService;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.domain.Room;
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
    private RoomQueryService roomQueryService;

    @Mock
    private RoomCommandService roomCommandService;

    @Mock
    private RoomStatusPort roomStatusPort;

    @Mock
    private Room room;

    @InjectMocks
    private RoomGameStartListener listener;

    @Test
    @DisplayName("GameSession 시작 이벤트를 받으면 인메모리 Room과 영속 RoomEntity를 모두 PLAYING으로 전이한다")
    void 게임_시작_이벤트로_방을_PLAYING_전이한다() {
        // given
        given(roomQueryService.getByJoinCode(new JoinCode("ABCD"))).willReturn(room);

        // when
        listener.handle(new GameSessionStartedEvent("ABCD"));

        // then
        verify(room).markPlaying();
        verify(roomStatusPort).updateStatus("ABCD", RoomState.PLAYING);
    }

    /**
     * 인메모리 저장소는 같은 참조를 들고 있어 save 없이도 전이가 반영되므로, save를 빠뜨려도 게임은 정상 동작한다.
     * 다만 방 참여 상태 변경 감지는 저장 지점에서만 일어나(RoomPresencePublisher) 친구 알림만 조용히 누락된다 —
     * RoomPresencePublisher 단위 테스트는 자기 호출자를 검증할 수 없으므로 그 연결을 여기서 고정한다(#1266 리뷰).
     */
    @Test
    @DisplayName("게임 시작 전이를 저장 지점에 반영해 친구 알림 감지가 누락되지 않게 한다")
    void 게임_시작_전이를_저장_지점에_반영한다() {
        // given
        given(roomQueryService.getByJoinCode(new JoinCode("ABCD"))).willReturn(room);

        // when
        listener.handle(new GameSessionStartedEvent("ABCD"));

        // then
        verify(roomCommandService).save(room);
    }
}
