package coffeeshout.minigame.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.fixture.PlayerFixture;
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
import coffeeshout.room.domain.player.Player;
import java.util.List;
import java.util.Map;
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
            Player 한스 = PlayerFixture.호스트한스();
            Player 루키 = PlayerFixture.게스트루키();

            MiniGameResult result = new MiniGameResult(Map.of(한스.toGamer(), 1, 루키.toGamer(), 2));
            Map<Gamer, MiniGameScore> scores = Map.of(
                    한스.toGamer(), new CardGameScore(100),
                    루키.toGamer(), new CardGameScore(80)
            );

            방과_미니게임_설정();
            게임세션_설정(result, scores);
            플레이어_설정(
                    new PlayerSnapshot("한스", 11L, 1L),
                    new PlayerSnapshot("루키", 22L, 2L)
            );

            listener.handle(미니게임종료이벤트(result));

            assertThat(발행된_통계().playerStats())
                    .containsExactlyInAnyOrder(new PlayerStat(1L, true), new PlayerStat(2L, false));
        }

        @Test
        void 게스트_플레이어는_userId가_null이므로_이벤트에서_제외된다() {
            Player 한스 = PlayerFixture.호스트한스();
            Player 루키 = PlayerFixture.게스트루키();

            MiniGameResult result = new MiniGameResult(Map.of(한스.toGamer(), 1, 루키.toGamer(), 2));
            Map<Gamer, MiniGameScore> scores = Map.of(
                    한스.toGamer(), new CardGameScore(100),
                    루키.toGamer(), new CardGameScore(80)
            );

            방과_미니게임_설정();
            게임세션_설정(result, scores);
            플레이어_설정(
                    new PlayerSnapshot("한스", 11L, 1L),    // 회원
                    new PlayerSnapshot("루키", 22L, null)   // 게스트
            );

            listener.handle(미니게임종료이벤트(result));

            assertThat(발행된_통계().playerStats())
                    .containsExactly(new PlayerStat(1L, true));
        }

        @Test
        void 전원_게스트인_방은_통계_이벤트가_발행되지_않는다() {
            Player 한스 = PlayerFixture.호스트한스();
            Player 루키 = PlayerFixture.게스트루키();

            MiniGameResult result = new MiniGameResult(Map.of(한스.toGamer(), 1, 루키.toGamer(), 2));
            Map<Gamer, MiniGameScore> scores = Map.of(
                    한스.toGamer(), new CardGameScore(100),
                    루키.toGamer(), new CardGameScore(80)
            );

            방과_미니게임_설정();
            게임세션_설정(result, scores);
            플레이어_설정(
                    new PlayerSnapshot("한스", 11L, null),
                    new PlayerSnapshot("루키", 22L, null)
            );

            listener.handle(미니게임종료이벤트(result));

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    private MiniGameStatsRecordedEvent 발행된_통계() {
        ArgumentCaptor<MiniGameStatsRecordedEvent> captor = ArgumentCaptor.forClass(MiniGameStatsRecordedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private MiniGameFinishedEvent 미니게임종료이벤트(MiniGameResult result) {
        return new MiniGameFinishedEvent(JOIN_CODE, MiniGameType.CARD_GAME.name(), result.toRankMap(), 1);
    }

    private void 방과_미니게임_설정() {
        when(roomSnapshotQuery.resolveRoomSessionId(JOIN_CODE)).thenReturn(ROOM_SESSION_ID);
        MiniGameEntity miniGameEntity = mock(MiniGameEntity.class);
        when(miniGameJpaRepository.findByRoomSessionIdAndMiniGameType(ROOM_SESSION_ID, MiniGameType.CARD_GAME))
                .thenReturn(java.util.Optional.of(miniGameEntity));
    }

    private void 게임세션_설정(MiniGameResult result, Map<Gamer, MiniGameScore> scores) {
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
}
