package coffeeshout.minigame.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.GameSessionStartedEvent;
import coffeeshout.minigame.event.GameStartReadyEvent;
import coffeeshout.minigame.event.MiniGameStartedEvent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MiniGameEventServiceTest {

    @Mock
    private GameSessionService gameSessionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MiniGamePersistenceService miniGamePersistenceService;

    @Mock
    private MiniGameService miniGameService;

    @Mock
    private Playable playable;

    private MiniGameEventService service;

    @BeforeEach
    void setUp() {
        given(miniGameService.getMiniGameType()).willReturn(MiniGameType.CARD_GAME);
        service = new MiniGameEventService(
                gameSessionService, List.of(miniGameService), eventPublisher, miniGamePersistenceService);
    }

    @Test
    @DisplayName("startGame 직후 GameSessionStartedEvent를 먼저 발행하고, 이후에 게임 시작·결과 영속을 수행한다")
    void 전이_이벤트를_실패_가능_IO보다_먼저_발행한다() {
        // given — 이 순서가 불변식이다: startGame → GameSessionStartedEvent(방 markPlaying) → start → save
        // 전이 이벤트가 start/save보다 먼저여야, 이후 I/O가 실패해도 GameSession·Room이 모두 PLAYING으로 일관된다.
        final GameStartReadyEvent event = new GameStartReadyEvent(
                "evt-1", "ABCD", "꾹이", List.of(Gamer.guest("꾹이")));
        given(gameSessionService.startGame(eq(new JoinCode("ABCD")), any(Gamer.class), anyList()))
                .willReturn(playable);
        given(playable.getMiniGameType()).willReturn(MiniGameType.CARD_GAME);

        // when
        service.onGameStartReady(event);

        // then
        final InOrder inOrder = inOrder(gameSessionService, eventPublisher, miniGameService, miniGamePersistenceService);
        inOrder.verify(gameSessionService).startGame(eq(new JoinCode("ABCD")), any(Gamer.class), anyList());
        inOrder.verify(eventPublisher).publishEvent(isA(GameSessionStartedEvent.class));
        inOrder.verify(miniGameService).start("ABCD", "꾹이");
        inOrder.verify(miniGamePersistenceService).saveGameEntities(event, MiniGameType.CARD_GAME);
        inOrder.verify(eventPublisher).publishEvent(isA(MiniGameStartedEvent.class));
    }
}
