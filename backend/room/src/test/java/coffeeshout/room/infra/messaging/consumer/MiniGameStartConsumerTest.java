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
        @DisplayName("검증 → GameStartReadyEvent 발행 → markPlaying 순서로 처리한다")
        void 검증_후_이벤트를_발행하고_markPlaying한다() {
            // given
            final StartMiniGameCommandEvent event = new StartMiniGameCommandEvent("ABCD", "꾹이");
            given(roomQueryService.getByJoinCode(new JoinCode("ABCD"))).willReturn(room);
            given(room.getGamers()).willReturn(List.of(Gamer.guest("꾹이"), Gamer.guest("초롱")));

            // when
            consumer.accept(event);

            // then — 검증(읽기) → 시작 위임(동기 발행) → PLAYING 전이 순서가 불변식
            final InOrder inOrder = inOrder(room, eventPublisher);
            inOrder.verify(room).validateStartable("꾹이");
            inOrder.verify(eventPublisher).publishEvent(any(GameStartReadyEvent.class));
            inOrder.verify(room).markPlaying();
        }

        @Test
        @DisplayName("게임 시작 위임이 예외로 실패하면 markPlaying을 호출하지 않는다")
        void 게임_시작_실패_시_markPlaying하지_않는다() {
            // given — 동기 리스너(:game startGame)의 실패를 발행 예외로 재현한다(예: 빈 대기열)
            final StartMiniGameCommandEvent event = new StartMiniGameCommandEvent("ABCD", "꾹이");
            given(roomQueryService.getByJoinCode(new JoinCode("ABCD"))).willReturn(room);
            given(room.getGamers()).willReturn(List.of(Gamer.guest("꾹이"), Gamer.guest("초롱")));
            willThrow(new IllegalStateException("시작할 수 있는 대기 게임이 없습니다."))
                    .given(eventPublisher).publishEvent(any(GameStartReadyEvent.class));

            // when & then
            assertThatThrownBy(() -> consumer.accept(event))
                    .isInstanceOf(IllegalStateException.class);
            verify(room, never()).markPlaying();
        }
    }
}
