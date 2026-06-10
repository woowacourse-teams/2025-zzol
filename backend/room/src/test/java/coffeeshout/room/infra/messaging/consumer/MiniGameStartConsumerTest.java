package coffeeshout.room.infra.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.event.GameStartReadyEvent;
import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.domain.Room;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MiniGameStartConsumerTest {

    @Mock
    private RoomQueryService roomQueryService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Room room;

    @InjectMocks
    private MiniGameStartConsumer consumer;

    @Nested
    @DisplayName("미니게임 시작 커맨드 처리(accept)")
    class Accept {

        @Test
        @DisplayName("방을 검증한 뒤 GameStartReadyEvent를 발행한다")
        void 검증_후_GameStartReadyEvent를_발행한다() {
            // given — markPlaying은 여기서 하지 않는다(GameSession 시작 직후 RoomGameStartListener가 수행)
            final StartMiniGameCommandEvent event = new StartMiniGameCommandEvent("ABCD", "꾹이");
            given(roomQueryService.getByJoinCode(new JoinCode("ABCD"))).willReturn(room);
            given(room.getGamers()).willReturn(List.of(Gamer.guest("꾹이"), Gamer.guest("초롱")));

            // when
            consumer.accept(event);

            // then — 검증(읽기) 후 시작 위임(이벤트 발행)
            final InOrder inOrder = inOrder(room, eventPublisher);
            inOrder.verify(room).validateStartable("꾹이");
            inOrder.verify(eventPublisher).publishEvent(any(GameStartReadyEvent.class));
        }

        @Test
        @DisplayName("방 검증에 실패하면 시작 이벤트를 발행하지 않는다")
        void 검증_실패시_이벤트를_발행하지_않는다() {
            // given
            final StartMiniGameCommandEvent event = new StartMiniGameCommandEvent("ABCD", "비호스트");
            given(roomQueryService.getByJoinCode(new JoinCode("ABCD"))).willReturn(room);
            willThrow(new IllegalStateException("게임을 시작할 수 있는 상태가 아닙니다."))
                    .given(room).validateStartable("비호스트");

            // when & then
            assertThatThrownBy(() -> consumer.accept(event))
                    .isInstanceOf(IllegalStateException.class);
            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
