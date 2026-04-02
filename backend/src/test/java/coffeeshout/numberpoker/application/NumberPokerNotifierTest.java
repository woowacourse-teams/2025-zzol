package coffeeshout.numberpoker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.global.websocket.ui.WebSocketResponse;
import coffeeshout.numberpoker.config.NumberPokerTimingProperties;
import coffeeshout.numberpoker.domain.NumberPokerGame;
import coffeeshout.numberpoker.ui.response.PokerStateMessage;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NumberPokerNotifierTest {

    @Mock
    LoggingSimpMessagingTemplate messagingTemplate;

    NumberPokerNotifier notifier;

    // firstLoading=3s, loading=2s, stage1=4s, stage2=8s, showdown=3s, scoreBoard=3s, roundReady=5s — ADR 명세 값
    final NumberPokerTimingProperties timing = new NumberPokerTimingProperties(
            Duration.ofSeconds(3), Duration.ofSeconds(2),
            Duration.ofSeconds(4), Duration.ofSeconds(8),
            Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(5));

    Player 꾹이 = PlayerFixture.호스트꾹이();
    Player 루키 = PlayerFixture.게스트루키();

    @BeforeEach
    void setUp() {
        notifier = new NumberPokerNotifier(messagingTemplate, timing);
    }

    @Nested
    class 페이즈_변경_브로드캐스트 {

        @Test
        void topic_poker_state로_현재_게임_상태를_전송한다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(deck -> Collections.shuffle(deck, new Random(42)));
            Room room = stubRoom("ABCD");

            notifier.notifyPhaseChanged(game, room);

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/ABCD/poker/state"),
                    any()
            );
        }

        @Test
        void STAGE_1_페이즈에서_timerSeconds_4가_포함된다() {
            NumberPokerGame game = gameAtStage1();
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            assertThat(message.timerSeconds()).isEqualTo(4);
        }

        @Test
        void STAGE_2_페이즈에서_timerSeconds_8이_포함된다() {
            NumberPokerGame game = gameAtStage2();
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            assertThat(message.timerSeconds()).isEqualTo(8);
        }

        @Test
        void ROUND_READY_페이즈에서_timerSeconds_5가_포함된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(deck -> Collections.shuffle(deck, new Random(42)));
            game.beginRoundReady();
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            assertThat(message.timerSeconds()).isEqualTo(5);
        }

        @Test
        void LOADING_페이즈에서_timerSeconds가_null이다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(deck -> Collections.shuffle(deck, new Random(42))); // LOADING
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            assertThat(message.timerSeconds()).isNull();
        }

        @Test
        void SHOWDOWN_페이즈에서_timerSeconds가_null이다() {
            NumberPokerGame game = gameAtShowdown();
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            assertThat(message.timerSeconds()).isNull();
        }

        @Test
        void SCORE_BOARD_전송_시_timerSeconds가_null이다() {
            NumberPokerGame game = gameAtShowdown();
            game.scoreBoard();
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureScoreBoardMessage(game, room, Map.of());

            assertThat(message.timerSeconds()).isNull();
        }
    }

    @Nested
    class 딜러_뒷면_카드_수 {

        @Test
        void LOADING_페이즈에서_dealerHiddenCount가_2이다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(deck -> Collections.shuffle(deck, new Random(42))); // LOADING: 딜러 뒷면 2장
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            assertThat(message.dealerHiddenCount()).isEqualTo(2);
            assertThat(message.dealerCards()).isEmpty();
        }

        @Test
        void STAGE_2_페이즈에서_dealerHiddenCount가_1이다() {
            NumberPokerGame game = gameAtStage2(); // 딜러 1장 공개
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            assertThat(message.dealerHiddenCount()).isEqualTo(1);
            assertThat(message.dealerCards()).hasSize(1);
        }

        @Test
        void SHOWDOWN_페이즈에서_dealerHiddenCount가_0이다() {
            NumberPokerGame game = gameAtShowdown(); // 딜러 2장 모두 공개
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            assertThat(message.dealerHiddenCount()).isZero();
            assertThat(message.dealerCards()).hasSize(2);
        }
    }

    @Nested
    class result_필드_노출_범위 {

        @Test
        void STAGE_1_페이즈에서_result가_null이다() {
            NumberPokerGame game = gameAtStage1();
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            message.players().forEach(p ->
                    assertThat(p.result())
                            .as("STAGE_1에서 %s의 result가 null이어야 한다", p.playerName())
                            .isNull()
            );
        }

        @Test
        void STAGE_2_페이즈에서_result가_null이다() {
            NumberPokerGame game = gameAtStage2();
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            message.players().forEach(p ->
                    assertThat(p.result())
                            .as("STAGE_2에서 %s의 result가 null이어야 한다", p.playerName())
                            .isNull()
            );
        }

        @Test
        void SHOWDOWN_페이즈에서_result가_포함된다() {
            NumberPokerGame game = gameAtShowdown();
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            message.players().forEach(p ->
                    assertThat(p.result())
                            .as("SHOWDOWN에서 %s의 result가 null이면 안 된다", p.playerName())
                            .isNotNull()
            );
        }

        @Test
        void SCORE_BOARD_페이즈에서_result가_포함된다() {
            NumberPokerGame game = gameAtShowdown();
            game.scoreBoard();
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureScoreBoardMessage(game, room, Map.of());

            message.players().forEach(p ->
                    assertThat(p.result())
                            .as("SCORE_BOARD에서 %s의 result가 null이면 안 된다", p.playerName())
                            .isNotNull()
            );
        }

        @Test
        void STAGE_1에서_폴드한_플레이어도_result는_null이다() {
            // folded:true 로 폴드 여부를 알 수 있으므로 result는 SCORE_BOARD까지 숨긴다
            NumberPokerGame game = gameAtStage1();
            game.fold(꾹이);
            Room room = stubRoom("ABCD");

            PokerStateMessage message = captureStateMessage(game, room);

            PokerStateMessage.PlayerInfo 꾹이Info = message.players().stream()
                    .filter(p -> p.playerName().equals("꾹이"))
                    .findFirst().orElseThrow();
            assertThat(꾹이Info.folded()).isTrue();
            assertThat(꾹이Info.result()).isNull();
        }
    }

    @Nested
    class 개인_패_전송 {

        @Test
        void LOADING_페이즈에서_각_플레이어에게_개인_패를_전송한다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(deck -> Collections.shuffle(deck, new Random(42)));
            Room room = stubRoom("ABCD");

            notifier.notifyHands(game, room);

            // StompPrincipalInterceptor가 Principal을 "joinCode:playerName" 형식으로 설정하므로
            // convertAndSendToUser 호출 시 PlayerKey 전체를 사용해야 한다
            verify(messagingTemplate).convertAndSendToUser(
                    eq("ABCD:꾹이"), eq("/queue/poker/hand"), any());
            verify(messagingTemplate).convertAndSendToUser(
                    eq("ABCD:루키"), eq("/queue/poker/hand"), any());
        }
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private NumberPokerGame gameAtStage1() {
        NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
        game.startRound(deck -> Collections.shuffle(deck, new Random(42)));
        game.beginStage1();
        return game;
    }

    private NumberPokerGame gameAtStage2() {
        NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
        game.startRound(deck -> Collections.shuffle(deck, new Random(42)));
        game.beginStage2();
        return game;
    }

    private NumberPokerGame gameAtShowdown() {
        NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
        game.startRound(deck -> Collections.shuffle(deck, new Random(42)));
        game.beginStage2();
        game.showdown();
        return game;
    }

    @SuppressWarnings("unchecked")
    private PokerStateMessage captureStateMessage(NumberPokerGame game, Room room) {
        AtomicReference<Object> captured = new AtomicReference<>();
        Mockito.doAnswer(inv -> { captured.set(inv.getArgument(1)); return null; })
                .when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));

        notifier.notifyPhaseChanged(game, room);

        return ((WebSocketResponse<PokerStateMessage>) captured.get()).data();
    }

    @SuppressWarnings("unchecked")
    private PokerStateMessage captureScoreBoardMessage(NumberPokerGame game, Room room,
                                                        Map<Player, Integer> deltas) {
        AtomicReference<Object> captured = new AtomicReference<>();
        Mockito.doAnswer(inv -> { captured.set(inv.getArgument(1)); return null; })
                .when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));

        notifier.notifyPhaseChanged(game, room, deltas);

        return ((WebSocketResponse<PokerStateMessage>) captured.get()).data();
    }

    private Room stubRoom(String joinCode) {
        Room room = Mockito.mock(Room.class);
        when(room.getJoinCode()).thenReturn(new JoinCode(joinCode));
        when(room.getPlayers()).thenReturn(List.of(꾹이, 루키));
        return room;
    }
}
