package coffeeshout.minigame.infra.messaging.consumer;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.minigame.event.dto.MiniGameSelectEvent;
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
class MiniGameSelectConsumerTest {

    @Mock
    private RoomQueryService roomQueryService;

    @Mock
    private GameSessionService gameSessionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MiniGameSelectConsumer consumer;

    @Nested
    @DisplayName("미니게임 선택 이벤트 처리(accept)")
    class Accept {

        @Test
        @DisplayName("호스트의 선택이면 세션을 갱신한 뒤 이벤트를 재발행한다")
        void 호스트의_선택이면_세션을_갱신한_뒤_이벤트를_재발행한다() {
            // given
            final Room room = RoomFixture.호스트_꾹이();
            given(roomQueryService.getByJoinCode(room.getJoinCode())).willReturn(room);
            final MiniGameSelectEvent event = new MiniGameSelectEvent(
                    room.getJoinCode().getValue(),
                    room.getHost().getName().value(),
                    List.of(MiniGameType.CARD_GAME)
            );

            // when
            consumer.accept(event);

            // then — 브로드캐스트는 세션 반영 성공 후에 일어나야 한다
            final InOrder inOrder = inOrder(gameSessionService, eventPublisher);
            inOrder.verify(gameSessionService).updateGames(event);
            inOrder.verify(eventPublisher).publishEvent(event);
        }

        @Test
        @DisplayName("호스트가 아니면 NOT_HOST 예외가 발생하고 세션 갱신·재발행이 일어나지 않는다")
        void 호스트가_아니면_세션_갱신과_재발행이_일어나지_않는다() {
            // given
            final Room room = RoomFixture.호스트_꾹이();
            given(roomQueryService.getByJoinCode(new JoinCode(room.getJoinCode().getValue()))).willReturn(room);
            final MiniGameSelectEvent event = new MiniGameSelectEvent(
                    room.getJoinCode().getValue(),
                    "비호스트",
                    List.of(MiniGameType.CARD_GAME)
            );

            // when & then
            assertCoffeeShoutException(
                    () -> consumer.accept(event),
                    RoomErrorCode.NOT_HOST
            );
            verifyNoInteractions(gameSessionService, eventPublisher);
        }
    }
}
