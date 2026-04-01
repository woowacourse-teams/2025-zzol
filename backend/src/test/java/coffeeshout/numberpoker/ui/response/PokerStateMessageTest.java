package coffeeshout.numberpoker.ui.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.numberpoker.domain.NumberPokerGame;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.roulette.Probability;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PokerStateMessageTest {

    Player 꾹이 = PlayerFixture.호스트꾹이();
    Player 루키 = PlayerFixture.게스트루키();

    Room room;

    @BeforeEach
    void setUp() {
        room = mock(Room.class);
        when(room.getJoinCode()).thenReturn(new JoinCode("ABCD"));
        when(room.getPlayers()).thenReturn(List.of(꾹이, 루키));
    }

    @Nested
    class 기본_필드 {

        @Test
        void 라운드_번호와_총_라운드_수가_포함된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.configureRoundCount(2);
            game.startRound(new Random(42));

            PokerStateMessage message = PokerStateMessage.from(game, room, (Integer) null);

            assertSoftly(softly -> {
                softly.assertThat(message.roundNumber()).isEqualTo(1);
                softly.assertThat(message.totalRounds()).isEqualTo(2);
            });
        }

        @Test
        void 현재_페이즈명이_문자열로_포함된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));
            game.beginStage1();

            PokerStateMessage message = PokerStateMessage.from(game, room, 4);

            assertThat(message.phase()).isEqualTo("STAGE_1");
        }

        @Test
        void 게임_시작_전에는_phase가_null이다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));

            PokerStateMessage message = PokerStateMessage.from(game, room, (Integer) null);

            assertThat(message.phase()).isNull();
        }

        @Test
        void 플레이어_목록에_모든_플레이어가_포함된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));

            PokerStateMessage message = PokerStateMessage.from(game, room, (Integer) null);

            assertThat(message.players()).extracting(PokerStateMessage.PlayerInfo::playerName)
                    .containsExactly("꾹이", "루키");
        }
    }

    @Nested
    class 딜러_카드_공개_상태 {

        @Test
        void LOADING_페이즈에서_공개_카드_없음_뒷면_2장() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));

            PokerStateMessage message = PokerStateMessage.from(game, room, (Integer) null);

            assertSoftly(softly -> {
                softly.assertThat(message.dealerCards()).isEmpty();
                softly.assertThat(message.dealerHiddenCount()).isEqualTo(2);
            });
        }

        @Test
        void STAGE_2_페이즈에서_공개_카드_1장_뒷면_1장() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));
            game.beginStage2(); // 딜러 1장 공개

            PokerStateMessage message = PokerStateMessage.from(game, room, 8);

            assertSoftly(softly -> {
                softly.assertThat(message.dealerCards()).hasSize(1);
                softly.assertThat(message.dealerHiddenCount()).isEqualTo(1);
            });
        }

        @Test
        void SHOWDOWN_페이즈에서_공개_카드_2장_뒷면_0장() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));
            game.beginStage2();
            game.showdown(); // 딜러 2장 전체 공개

            PokerStateMessage message = PokerStateMessage.from(game, room, (Integer) null);

            assertSoftly(softly -> {
                softly.assertThat(message.dealerCards()).hasSize(2);
                softly.assertThat(message.dealerHiddenCount()).isEqualTo(0);
            });
        }
    }

    @Nested
    class result_노출_범위 {

        @Test
        void LOADING_페이즈에서_전_플레이어_result_null() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));

            PokerStateMessage message = PokerStateMessage.from(game, room, (Integer) null);

            message.players().forEach(p ->
                    assertThat(p.result()).as("%s LOADING result", p.playerName()).isNull());
        }

        @Test
        void STAGE_1_페이즈에서_전_플레이어_result_null() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));
            game.beginStage1();

            PokerStateMessage message = PokerStateMessage.from(game, room, 4);

            message.players().forEach(p ->
                    assertThat(p.result()).as("%s STAGE_1 result", p.playerName()).isNull());
        }

        @Test
        void STAGE_2_페이즈에서_전_플레이어_result_null() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));
            game.beginStage2();

            PokerStateMessage message = PokerStateMessage.from(game, room, 8);

            message.players().forEach(p ->
                    assertThat(p.result()).as("%s STAGE_2 result", p.playerName()).isNull());
        }

        @Test
        void SHOWDOWN_페이즈에서_전_플레이어_result_포함() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));
            game.beginStage2();
            game.showdown();

            PokerStateMessage message = PokerStateMessage.from(game, room, (Integer) null);

            message.players().forEach(p ->
                    assertThat(p.result()).as("%s SHOWDOWN result", p.playerName()).isNotNull());
        }

        @Test
        void SCORE_BOARD_페이즈에서_전_플레이어_result_포함() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));
            game.beginStage2();
            game.showdown();
            game.scoreBoard();

            PokerStateMessage message = PokerStateMessage.from(game, room, Map.of());

            message.players().forEach(p ->
                    assertThat(p.result()).as("%s SCORE_BOARD result", p.playerName()).isNotNull());
        }

        @Test
        void STAGE_1_폴드한_플레이어도_result는_null이고_folded는_true() {
            // folded:true 로 폴드 여부를 표현하므로 result는 SCORE_BOARD 전까지 숨긴다
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));
            game.beginStage1();
            game.fold(꾹이);

            PokerStateMessage message = PokerStateMessage.from(game, room, 4);

            PokerStateMessage.PlayerInfo 꾹이Info = playerInfoOf(message, "꾹이");
            assertSoftly(softly -> {
                softly.assertThat(꾹이Info.folded()).isTrue();
                softly.assertThat(꾹이Info.result()).isNull();
            });
        }
    }

    @Nested
    class 플레이어_상태_필드 {

        @Test
        void ROUND_READY에서_레디한_플레이어는_ready_true() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));
            game.beginRoundReady();
            game.markReady(꾹이);

            PokerStateMessage message = PokerStateMessage.from(game, room, 5);

            PokerStateMessage.PlayerInfo 꾹이Info = playerInfoOf(message, "꾹이");
            PokerStateMessage.PlayerInfo 루키Info = playerInfoOf(message, "루키");
            assertSoftly(softly -> {
                softly.assertThat(꾹이Info.ready()).isTrue();
                softly.assertThat(루키Info.ready()).isFalse();
            });
        }

        @Test
        void 플레이어_probability_값이_포함된다() {
            꾹이.updateProbability(new Probability(3000));
            루키.updateProbability(new Probability(7000));
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));

            PokerStateMessage message = PokerStateMessage.from(game, room, (Integer) null);

            assertSoftly(softly -> {
                softly.assertThat(playerInfoOf(message, "꾹이").probability()).isEqualTo(3000);
                softly.assertThat(playerInfoOf(message, "루키").probability()).isEqualTo(7000);
            });
        }

        @Test
        void probability_미설정_플레이어는_probability_null() {
            // probability가 설정되지 않은 Player는 null 반환
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));

            PokerStateMessage message = PokerStateMessage.from(game, room, (Integer) null);

            message.players().forEach(p ->
                    assertThat(p.probability()).as("%s probability", p.playerName()).isNull());
        }
    }

    @Nested
    class SCORE_BOARD_확률_변동량 {

        @Test
        void 플레이어별_probabilityDelta가_포함된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));
            game.beginStage2();
            game.showdown();
            game.scoreBoard();

            Map<Player, Integer> deltas = Map.of(꾹이, -100, 루키, 100);

            PokerStateMessage message = PokerStateMessage.from(game, room, deltas);

            assertSoftly(softly -> {
                softly.assertThat(playerInfoOf(message, "꾹이").probabilityDelta()).isEqualTo(-100);
                softly.assertThat(playerInfoOf(message, "루키").probabilityDelta()).isEqualTo(100);
            });
        }

        @Test
        void delta_없는_플레이어는_probabilityDelta_null() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.startRound(new Random(42));
            game.beginStage2();
            game.showdown();
            game.scoreBoard();

            PokerStateMessage message = PokerStateMessage.from(game, room, Map.of());

            message.players().forEach(p ->
                    assertThat(p.probabilityDelta()).as("%s delta", p.playerName()).isNull());
        }
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private PokerStateMessage.PlayerInfo playerInfoOf(PokerStateMessage message, String name) {
        return message.players().stream()
                .filter(p -> p.playerName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("플레이어를 찾을 수 없음: " + name));
    }
}
