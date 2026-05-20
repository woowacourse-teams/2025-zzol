package coffeeshout.minigame.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.Playable;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.minigame.infra.persistence.MiniGameResultJpaRepository;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
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

    @InjectMocks
    MiniGameResultSaveEventListener listener;

    @Mock
    RoomJpaRepository roomJpaRepository;
    @Mock
    PlayerJpaRepository playerJpaRepository;
    @Mock
    MiniGameJpaRepository miniGameJpaRepository;
    @Mock
    MiniGameResultJpaRepository miniGameResultJpaRepository;
    @Mock
    RoomQueryService roomQueryService;
    @Mock
    RoomCommandService roomCommandService;
    @Mock
    GameSessionService gameSessionService;
    @Mock
    UserStatsService userStatsService;

    private static final String JOIN_CODE = "AB3C";

    @Nested
    class 게임_종료_시_UserStats_자동_업데이트 {

        @Test
        void 회원_플레이어는_1위면_isWinner_true로_UserStats가_업데이트된다() {
            PlayerName 한스 = new PlayerName("한스");
            PlayerName 루키 = new PlayerName("루키");

            MiniGameResult result = new MiniGameResult(Map.of(한스, 1, 루키, 2));
            Map<PlayerName, MiniGameScore> scores = Map.of(
                    한스, new CardGameScore(100),
                    루키, new CardGameScore(80)
            );

            RoomEntity roomEntity = 룸엔티티_설정();
            미니게임엔티티_설정(roomEntity);
            도메인룸_설정(List.of(한스, 루키), result, scores);

            PlayerEntity 한스Entity = 플레이어엔티티("한스", 1L);
            PlayerEntity 루키Entity = 플레이어엔티티("루키", 2L);
            when(playerJpaRepository.findByRoomSessionAndPlayerNameIn(eq(roomEntity), any()))
                    .thenReturn(List.of(한스Entity, 루키Entity));

            listener.handle(new MiniGameFinishedEvent(JOIN_CODE, MiniGameType.CARD_GAME.name()));

            verify(userStatsService).updateStats(1L, true);
            verify(userStatsService).updateStats(2L, false);
        }

        @Test
        void 게스트_플레이어는_userId가_null이므로_UserStats_업데이트에서_제외된다() {
            PlayerName 한스 = new PlayerName("한스");
            PlayerName 루키 = new PlayerName("루키");

            MiniGameResult result = new MiniGameResult(Map.of(한스, 1, 루키, 2));
            Map<PlayerName, MiniGameScore> scores = Map.of(
                    한스, new CardGameScore(100),
                    루키, new CardGameScore(80)
            );

            RoomEntity roomEntity = 룸엔티티_설정();
            미니게임엔티티_설정(roomEntity);
            도메인룸_설정(List.of(한스, 루키), result, scores);

            PlayerEntity 한스Entity = 플레이어엔티티("한스", 1L);   // 회원
            PlayerEntity 루키Entity = 플레이어엔티티("루키", null);  // 게스트
            when(playerJpaRepository.findByRoomSessionAndPlayerNameIn(eq(roomEntity), any()))
                    .thenReturn(List.of(한스Entity, 루키Entity));

            listener.handle(new MiniGameFinishedEvent(JOIN_CODE, MiniGameType.CARD_GAME.name()));

            verify(userStatsService).updateStats(1L, true);
            verify(userStatsService, never()).updateStats(eq(null), anyBoolean());
        }

        @Test
        void 전원_게스트인_방은_UserStats_업데이트가_발생하지_않는다() {
            PlayerName 한스 = new PlayerName("한스");
            PlayerName 루키 = new PlayerName("루키");

            MiniGameResult result = new MiniGameResult(Map.of(한스, 1, 루키, 2));
            Map<PlayerName, MiniGameScore> scores = Map.of(
                    한스, new CardGameScore(100),
                    루키, new CardGameScore(80)
            );

            RoomEntity roomEntity = 룸엔티티_설정();
            미니게임엔티티_설정(roomEntity);
            도메인룸_설정(List.of(한스, 루키), result, scores);

            PlayerEntity 한스Entity = 플레이어엔티티("한스", null);
            PlayerEntity 루키Entity = 플레이어엔티티("루키", null);
            when(playerJpaRepository.findByRoomSessionAndPlayerNameIn(eq(roomEntity), any()))
                    .thenReturn(List.of(한스Entity, 루키Entity));

            listener.handle(new MiniGameFinishedEvent(JOIN_CODE, MiniGameType.CARD_GAME.name()));

            verify(userStatsService, never()).updateStats(any(), anyBoolean());
        }
    }

    private RoomEntity 룸엔티티_설정() {
        RoomEntity roomEntity = mock(RoomEntity.class);
        when(roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc(JOIN_CODE))
                .thenReturn(Optional.of(roomEntity));
        return roomEntity;
    }

    private void 미니게임엔티티_설정(RoomEntity roomEntity) {
        MiniGameEntity miniGameEntity = mock(MiniGameEntity.class);
        when(miniGameEntity.getMiniGameType()).thenReturn(MiniGameType.CARD_GAME);
        when(miniGameJpaRepository.findByRoomSessionAndMiniGameType(roomEntity, MiniGameType.CARD_GAME))
                .thenReturn(Optional.of(miniGameEntity));
    }

    private void 도메인룸_설정(List<PlayerName> playerNames, MiniGameResult result, Map<PlayerName, MiniGameScore> scores) {
        Playable miniGame = mock(Playable.class);
        when(miniGame.getResult()).thenReturn(result);
        when(miniGame.getScores()).thenReturn(scores);

        GameSession session = mock(GameSession.class);
        when(session.findCompletedGame(MiniGameType.CARD_GAME)).thenReturn(miniGame);
        when(gameSessionService.getSession(new JoinCode(JOIN_CODE))).thenReturn(session);

        List<Player> players = playerNames.stream()
                .map(Player::createGuest)
                .toList();

        Room room = mock(Room.class);
        when(room.getPlayers()).thenReturn(players);
        when(roomQueryService.getByJoinCode(new JoinCode(JOIN_CODE))).thenReturn(room);
    }

    private PlayerEntity 플레이어엔티티(String name, Long userId) {
        PlayerEntity entity = mock(PlayerEntity.class);
        when(entity.getPlayerName()).thenReturn(name);
        when(entity.getUserId()).thenReturn(userId);
        return entity;
    }
}
