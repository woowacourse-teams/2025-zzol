package coffeeshout.minigame.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.Playable;
import coffeeshout.gamecommon.RoomSnapshotQuery;
import coffeeshout.gamecommon.RoomSnapshotQuery.PlayerSnapshot;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.minigame.event.dto.MiniGameStatsRecordedEvent;
import coffeeshout.minigame.event.dto.MiniGameStatsRecordedEvent.PlayerStat;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.minigame.infra.persistence.MiniGameResultJpaRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MiniGameResultSaveEventListenerTest {

    @InjectMocks
    MiniGameResultSaveEventListener listener;

    @Mock
    RoomSnapshotQuery roomSnapshotQuery;
    @Mock
    MiniGameJpaRepository miniGameJpaRepository;
    @Mock
    MiniGameResultJpaRepository miniGameResultJpaRepository;
    @Mock
    GameSessionService gameSessionService;
    @Mock
    ApplicationEventPublisher eventPublisher;

    private static final String JOIN_CODE = "AB3C";
    private static final long ROOM_SESSION_ID = 7L;

    @Nested
    class 게임_종료_시_통계_이벤트가_발행된다 {

        @Test
        void 회원_플레이어는_1위면_isWinner_true로_이벤트에_담긴다() {
            Gamer 한스 = Gamer.loggedIn("한스", 1L);
            Gamer 루키 = Gamer.loggedIn("루키", 2L);

            게임_결과_설정(한스, 루키);
            플레이어_설정(new PlayerSnapshot("한스", 11L), new PlayerSnapshot("루키", 22L));

            listener.handle(미니게임종료이벤트(한스, 루키));

            assertThat(발행된_통계().playerStats())
                    .containsExactlyInAnyOrder(new PlayerStat(1L, true), new PlayerStat(2L, false));
        }

        @Test
        void 게스트_플레이어는_userId가_null이므로_이벤트에서_제외된다() {
            Gamer 한스 = Gamer.loggedIn("한스", 1L);
            Gamer 루키 = Gamer.guest("루키");

            게임_결과_설정(한스, 루키);
            플레이어_설정(new PlayerSnapshot("한스", 11L), new PlayerSnapshot("루키", 22L));

            listener.handle(미니게임종료이벤트(한스, 루키));

            assertThat(발행된_통계().playerStats())
                    .containsExactly(new PlayerStat(1L, true));
        }

        @Test
        void 전원_게스트인_방은_통계_이벤트가_발행되지_않는다() {
            Gamer 한스 = Gamer.guest("한스");
            Gamer 루키 = Gamer.guest("루키");

            게임_결과_설정(한스, 루키);
            플레이어_설정(new PlayerSnapshot("한스", 11L), new PlayerSnapshot("루키", 22L));

            listener.handle(미니게임종료이벤트(한스, 루키));

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    private void 게임_결과_설정(Gamer 한스, Gamer 루키) {
        MiniGameResult result = new MiniGameResult(Map.of(한스, 1, 루키, 2));
        Map<Gamer, MiniGameScore> scores = Map.of(
                한스, new CardGameScore(100),
                루키, new CardGameScore(80)
        );

        when(roomSnapshotQuery.resolveRoomSessionId(JOIN_CODE)).thenReturn(ROOM_SESSION_ID);
        MiniGameEntity miniGameEntity = mock(MiniGameEntity.class);
        when(miniGameJpaRepository.findByRoomSessionIdAndMiniGameType(ROOM_SESSION_ID, MiniGameType.CARD_GAME))
                .thenReturn(Optional.of(miniGameEntity));

        Playable miniGame = mock(Playable.class);
        when(miniGame.getResult()).thenReturn(result);
        when(miniGame.getScores()).thenReturn(scores);

        GameSession session = mock(GameSession.class);
        when(session.findCompletedGame(MiniGameType.CARD_GAME)).thenReturn(miniGame);
        when(gameSessionService.getSession(new JoinCode(JOIN_CODE))).thenReturn(session);
    }

    private void 플레이어_설정(PlayerSnapshot... snapshots) {
        when(roomSnapshotQuery.resolvePlayers(eq(ROOM_SESSION_ID), any()))
                .thenReturn(List.of(snapshots));
    }

    private MiniGameStatsRecordedEvent 발행된_통계() {
        ArgumentCaptor<MiniGameStatsRecordedEvent> captor = ArgumentCaptor.forClass(MiniGameStatsRecordedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private MiniGameFinishedEvent 미니게임종료이벤트(Gamer 한스, Gamer 루키) {
        MiniGameResult result = new MiniGameResult(Map.of(한스, 1, 루키, 2));
        return new MiniGameFinishedEvent(JOIN_CODE, MiniGameType.CARD_GAME.name(), result.toRankMap(), 1);
    }
}
