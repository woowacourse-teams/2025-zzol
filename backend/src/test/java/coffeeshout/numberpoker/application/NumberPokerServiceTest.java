package coffeeshout.numberpoker.application;

import static coffeeshout.global.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.numberpoker.domain.NumberPokerErrorCode;
import coffeeshout.numberpoker.domain.NumberPokerGame;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomQueryService;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class NumberPokerServiceTest {

    @Mock
    RoomQueryService roomQueryService;

    @Mock
    NumberPokerGameStore gameStore;

    @Mock
    NumberPokerFlowOrchestrator flowOrchestrator;

    @Mock
    NumberPokerNotifier notifier;

    @InjectMocks
    NumberPokerService service;

    Player 꾹이 = PlayerFixture.호스트꾹이();
    Player 루키 = PlayerFixture.게스트루키();

    Room room;
    NumberPokerGame game;

    @BeforeEach
    void setUp() {
        room = mock(Room.class);
        game = new NumberPokerGame(List.of(꾹이, 루키));
        game.startRound(new Random(42));
        game.beginStage1();

        when(roomQueryService.getByJoinCode(new JoinCode("ABCD"))).thenReturn(room);
        when(gameStore.get("ABCD")).thenReturn(game);
        when(room.getPlayers()).thenReturn(List.of(꾹이, 루키));
        when(room.findPlayer(new PlayerName("꾹이"))).thenReturn(꾹이);
        when(room.findPlayer(new PlayerName("루키"))).thenReturn(루키);
        when(room.isHost(꾹이)).thenReturn(true);
        when(room.isHost(루키)).thenReturn(false);
        when(room.getJoinCode()).thenReturn(new JoinCode("ABCD"));
    }

    @Nested
    class 게임_시작 {

        @Test
        void start_호출_시_게임을_저장하고_오케스트레이터를_시작한다() {
            Room newRoom = mock(Room.class);
            NumberPokerGame newGame = new NumberPokerGame(List.of(꾹이, 루키));
            when(roomQueryService.getByJoinCode(new JoinCode("HJKL"))).thenReturn(newRoom);
            when(newRoom.findMiniGame(MiniGameType.NUMBER_POKER)).thenReturn(newGame);

            service.start("HJKL", "꾹이");

            verify(gameStore).save(eq("HJKL"), eq(newGame));
            verify(flowOrchestrator).startFlow(eq(newGame), eq(newRoom));
        }
    }

    @Nested
    class 폴드_처리 {

        @Test
        void 플레이어가_폴드하면_게임에_폴드가_반영된다() {
            service.fold("ABCD", "꾹이");

            assertThat(game.isPlayerFolded(꾹이)).isTrue();
        }

        @Test
        void 폴드_후_알림이_전송된다() {
            service.fold("ABCD", "꾹이");

            verify(notifier).notifyPhaseChanged(game, room);
        }

        @Test
        void 이미_폴드한_플레이어가_다시_폴드하면_예외가_발생한다() {
            game.fold(꾹이);

            assertCoffeeShoutException(
                    () -> service.fold("ABCD", "꾹이"),
                    NumberPokerErrorCode.ALREADY_FOLDED
            );
        }
    }

    @Nested
    class 레디_처리 {

        @BeforeEach
        void 라운드를_ROUND_READY까지_진행() {
            game.beginStage2();
            game.showdown();
            game.scoreBoard();
            game.beginRoundReady();
        }

        @Test
        void 플레이어가_레디하면_게임에_반영된다() {
            service.ready("ABCD", "꾹이");

            assertThat(game.isPlayerReady(꾹이)).isTrue();
        }

        @Test
        void 레디_후_알림이_전송된다() {
            service.ready("ABCD", "꾹이");

            verify(notifier).notifyPhaseChanged(game, room);
        }

        @Test
        void 전원_레디시_조기_종료_트리거가_발동된다() {
            service.ready("ABCD", "꾹이");
            service.ready("ABCD", "루키");

            verify(flowOrchestrator).triggerEarlyRoundReady("ABCD");
        }

        @Test
        void 전원_미만_레디시_트리거가_발동되지_않는다() {
            service.ready("ABCD", "꾹이");

            verify(flowOrchestrator, never()).triggerEarlyRoundReady(any());
        }

        @Test
        void 전원_레디시_레디_횟수만큼_알림이_전송된다() {
            service.ready("ABCD", "꾹이");
            service.ready("ABCD", "루키");

            // 각 ready() 호출마다 알림 전송 — 2번 호출
            verify(notifier, times(2)).notifyPhaseChanged(game, room);
        }
    }

    @Nested
    class 라운드_수_설정 {

        @Test
        void 호스트가_라운드_수를_변경할_수_있다() {
            NumberPokerGame freshGame = new NumberPokerGame(List.of(꾹이, 루키));
            when(gameStore.get("ABCD")).thenReturn(freshGame);

            service.configureRoundCount("ABCD", "꾹이", 5);

            assertThat(freshGame.getTotalRounds()).isEqualTo(5);
        }

        @Test
        void 호스트가_아닌_플레이어가_설정하면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> service.configureRoundCount("ABCD", "루키", 5),
                    NumberPokerErrorCode.NOT_HOST
            );
        }
    }
}
