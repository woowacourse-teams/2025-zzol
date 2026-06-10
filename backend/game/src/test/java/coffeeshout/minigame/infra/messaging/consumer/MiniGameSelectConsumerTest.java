package coffeeshout.minigame.infra.messaging.consumer;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.GameSessionErrorCode;
import coffeeshout.minigame.domain.MiniGameType;
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
    private GameSessionService gameSessionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MiniGameSelectConsumer consumer;

    @Nested
    @DisplayName("미니게임 선택 이벤트 처리(accept)")
    class Accept {

        @Test
        @DisplayName("세션을 갱신한 뒤 이벤트를 재발행한다")
        void 세션을_갱신한_뒤_이벤트를_재발행한다() {
            // given
            final MiniGameSelectEvent event = new MiniGameSelectEvent(
                    "ABCD",
                    "꾹이",
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
        @DisplayName("세션 갱신이 실패하면(비호스트 등) 이벤트가 재발행되지 않는다")
        void 세션_갱신이_실패하면_이벤트가_재발행되지_않는다() {
            // given — 호스트 검증은 GameSession(updateGames 내부)이 수행하므로 실패를 위임 모킹으로 재현한다
            final MiniGameSelectEvent event = new MiniGameSelectEvent(
                    "ABCD",
                    "비호스트",
                    List.of(MiniGameType.CARD_GAME)
            );
            willThrow(new BusinessException(GameSessionErrorCode.NOT_HOST, "호스트만 수행할 수 있는 작업입니다."))
                    .given(gameSessionService).updateGames(event);

            // when & then
            assertCoffeeShoutException(
                    () -> consumer.accept(event),
                    GameSessionErrorCode.NOT_HOST
            );
            verify(eventPublisher, never()).publishEvent(event);
        }
    }
}
