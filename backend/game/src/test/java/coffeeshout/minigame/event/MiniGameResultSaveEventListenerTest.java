package coffeeshout.minigame.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import coffeeshout.gamecommon.PlayerRef;
import coffeeshout.gamecommon.RoomReferencePort;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.minigame.infra.persistence.MiniGameResultJpaRepository;
import coffeeshout.room.domain.player.Player;
import coffeeshout.user.application.service.UserStatsService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MiniGameResultSaveEventListenerTest {

    private static final String JOIN_CODE = "AB3C";
    private static final Long ROOM_SESSION_ID = 1L;

    @InjectMocks
    MiniGameResultSaveEventListener listener;

    @Mock
    RoomReferencePort roomReferencePort;
    @Mock
    MiniGameJpaRepository miniGameJpaRepository;
    @Mock
    MiniGameResultJpaRepository miniGameResultJpaRepository;
    @Mock
    GameSessionService gameSessionService;
    @Mock
    UserStatsService userStatsService;

    @Nested
    class 게임_종료_시_UserStats_자동_업데이트 {

        @Test
        void 회원_플레이어는_1위면_isWinner_true로_UserStats가_업데이트된다() {
            Player 한스 = PlayerFixture.호스트한스();
            Player 루키 = PlayerFixture.게스트루키();

            MiniGameResult result = new MiniGameResult(Map.of(한스.toGamer(), 1, 루키.toGamer(), 2));
            Map<Gamer, MiniGameScore> scores = Map.of(
                    한스.toGamer(), new CardGameScore(100),
                    루키.toGamer(), new CardGameScore(80)
            );

            룸세션_설정();
            미니게임엔티티_설정();
            게임세션_설정(result, scores);

            플레이어참조_설정(
                    new PlayerRef(10L, 한스.toGamer().getName(), 1L),
                    new PlayerRef(20L, 루키.toGamer().getName(), 2L)
            );

            listener.handle(미니게임종료이벤트(result));

            verify(userStatsService).updateStats(1L, true);
            verify(userStatsService).updateStats(2L, false);
        }

        @Test
        void 게스트_플레이어는_userId가_null이므로_UserStats_업데이트에서_제외된다() {
            Player 한스 = PlayerFixture.호스트한스();
            Player 루키 = PlayerFixture.게스트루키();

            MiniGameResult result = new MiniGameResult(Map.of(한스.toGamer(), 1, 루키.toGamer(), 2));
            Map<Gamer, MiniGameScore> scores = Map.of(
                    한스.toGamer(), new CardGameScore(100),
                    루키.toGamer(), new CardGameScore(80)
            );

            룸세션_설정();
            미니게임엔티티_설정();
            게임세션_설정(result, scores);

            플레이어참조_설정(
                    new PlayerRef(10L, 한스.toGamer().getName(), 1L),    // 회원
                    new PlayerRef(20L, 루키.toGamer().getName(), null)   // 게스트
            );

            listener.handle(미니게임종료이벤트(result));

            verify(userStatsService).updateStats(1L, true);
            verify(userStatsService, never()).updateStats(eq(null), anyBoolean());
        }

        @Test
        void 전원_게스트인_방은_UserStats_업데이트가_발생하지_않는다() {
            Player 한스 = PlayerFixture.호스트한스();
            Player 루키 = PlayerFixture.게스트루키();

            MiniGameResult result = new MiniGameResult(Map.of(한스.toGamer(), 1, 루키.toGamer(), 2));
            Map<Gamer, MiniGameScore> scores = Map.of(
                    한스.toGamer(), new CardGameScore(100),
                    루키.toGamer(), new CardGameScore(80)
            );

            룸세션_설정();
            미니게임엔티티_설정();
            게임세션_설정(result, scores);

            플레이어참조_설정(
                    new PlayerRef(10L, 한스.toGamer().getName(), null),
                    new PlayerRef(20L, 루키.toGamer().getName(), null)
            );

            listener.handle(미니게임종료이벤트(result));

            verify(userStatsService, never()).updateStats(any(), anyBoolean());
        }
    }

    private MiniGameFinishedEvent 미니게임종료이벤트(MiniGameResult result) {
        return new MiniGameFinishedEvent(JOIN_CODE, MiniGameType.CARD_GAME.name(), result.toRankMap(), 1);
    }

    private void 룸세션_설정() {
        when(roomReferencePort.findCurrentRoomSessionId(JOIN_CODE))
                .thenReturn(Optional.of(ROOM_SESSION_ID));
    }

    private void 미니게임엔티티_설정() {
        MiniGameEntity miniGameEntity = mock(MiniGameEntity.class);
        when(miniGameJpaRepository.findByRoomSessionIdAndMiniGameType(ROOM_SESSION_ID, MiniGameType.CARD_GAME))
                .thenReturn(Optional.of(miniGameEntity));
    }

    private void 게임세션_설정(MiniGameResult result, Map<Gamer, MiniGameScore> scores) {
        Playable miniGame = mock(Playable.class);
        when(miniGame.getResult()).thenReturn(result);
        when(miniGame.getScores()).thenReturn(scores);

        GameSession session = mock(GameSession.class);
        when(session.findCompletedGame(MiniGameType.CARD_GAME)).thenReturn(miniGame);
        when(gameSessionService.getSession(new JoinCode(JOIN_CODE))).thenReturn(session);
    }

    private void 플레이어참조_설정(PlayerRef... refs) {
        when(roomReferencePort.findPlayerRefs(eq(ROOM_SESSION_ID), any()))
                .thenReturn(List.of(refs));
    }
}
