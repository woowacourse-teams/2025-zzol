package coffeeshout.minigame.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.RoomReferencePort;
import coffeeshout.minigame.application.port.MiniGameEntityRepository;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.GameStartReadyEvent;
import coffeeshout.minigame.event.PlayerSnapshotRequiredEvent;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MiniGamePersistenceServiceTest {

    private static final String JOIN_CODE = "ABCD";
    private static final Long ROOM_SESSION_ID = 1L;

    @Mock
    private GameSessionService gameSessionService;

    @Mock
    private RoomReferencePort roomReferencePort;

    @Mock
    private MiniGameEntityRepository miniGameEntityRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GameSession gameSession;

    private MiniGamePersistenceService service;

    @BeforeEach
    void setUp() {
        service = new MiniGamePersistenceService(
                gameSessionService,
                roomReferencePort,
                miniGameEntityRepository,
                eventPublisher
        );
    }

    private GameStartReadyEvent gameStartReadyEvent() {
        return new GameStartReadyEvent("evt-1", JOIN_CODE, "꾹이", List.of(Gamer.guest("꾹이")));
    }

    @Nested
    @DisplayName("게임 시작 엔티티를 저장할 때")
    class 게임_시작_저장 {

        @BeforeEach
        void 방과_세션을_스텁한다() {
            given(roomReferencePort.findCurrentRoomSessionId(JOIN_CODE))
                    .willReturn(Optional.of(ROOM_SESSION_ID));
            given(gameSessionService.getSession(new JoinCode(JOIN_CODE))).willReturn(gameSession);
        }

        @Test
        @DisplayName("첫 게임이면 PlayerEntity를 직접 만들지 않고 PlayerSnapshotRequiredEvent를 발행한다")
        void 첫_게임이면_스냅샷_이벤트를_발행한다() {
            // given
            given(gameSession.isFirstGameStarted()).willReturn(true);

            // when
            service.saveGameEntities(gameStartReadyEvent(), MiniGameType.CARD_GAME);

            // then
            final ArgumentCaptor<PlayerSnapshotRequiredEvent> captor =
                    ArgumentCaptor.forClass(PlayerSnapshotRequiredEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            Assertions.assertThat(captor.getValue().joinCode()).isEqualTo(JOIN_CODE);
        }

        @Test
        @DisplayName("첫 게임이 아니면 스냅샷 이벤트를 발행하지 않는다")
        void 첫_게임이_아니면_발행하지_않는다() {
            // given
            given(gameSession.isFirstGameStarted()).willReturn(false);

            // when
            service.saveGameEntities(gameStartReadyEvent(), MiniGameType.CARD_GAME);

            // then
            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
