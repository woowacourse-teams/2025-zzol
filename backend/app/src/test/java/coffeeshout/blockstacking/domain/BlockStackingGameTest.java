package coffeeshout.blockstacking.domain;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayersFixture;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.minigame.domain.GamerErrorCode;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BlockStackingGameTest {

    // 기본 탭 좌표: 정상 overlap(135px) 발생 케이스
    // movingBlock: x=100, width=150 → right=250
    // stackTop:    x=85,  width=150 → right=235
    // overlap = min(250,235) - max(100,85) = 235 - 100 = 135
    static final double MOVING_BLOCK_X = 100.0;
    static final double STACK_TOP_X = 85.0;
    static final double STACK_TOP_WIDTH = 150.0;

    BlockStackingGame game;
    PlayerName 꾹이;
    PlayerName 루키;
    PlayerName 엠제이;
    PlayerName 한스;
    List<PlayerName> players;

    @BeforeEach
    void setUp() {
        꾹이 = new PlayerName("꾹이");
        루키 = new PlayerName("루키");
        엠제이 = new PlayerName("엠제이");
        한스 = new PlayerName("한스");
        players = PlayersFixture.호스트꾹이_루키_엠제이_한스.getPlayers().stream()
                .map(Player::getName).toList();

        game = new BlockStackingGame();
        game.setUp(players.stream().map(name -> new Gamer(name, null)).toList());
    }

    @Nested
    class 게임_초기화_테스트 {

        @Test
        void READY_상태로_시작한다() {
            assertThat(game.getState()).isEqualTo(BlockStackingGameState.READY);
        }

        @Test
        void setUp_후_모든_플레이어_floor가_0이다() {
            game.prepare();
            game.startPlay();

            final Map<Gamer, MiniGameScore> scores = game.getScores();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(scores).hasSize(4);
                softly.assertThat(scores.get(GamerFixture.게스트꾹이()).getValue()).isZero();
                softly.assertThat(scores.get(GamerFixture.게스트루키()).getValue()).isZero();
                softly.assertThat(scores.get(GamerFixture.게스트엠제이()).getValue()).isZero();
                softly.assertThat(scores.get(GamerFixture.게스트한스()).getValue()).isZero();
            });
        }

        @Test
        void getMiniGameType이_BLOCK_STACKING을_반환한다() {
            assertThat(game.getMiniGameType()).isEqualTo(MiniGameType.BLOCK_STACKING);
        }
    }

    @Nested
    class 상태_전환_테스트 {

        @Test
        void prepare_호출_시_PREPARE_상태가_된다() {
            game.prepare();

            assertThat(game.getState()).isEqualTo(BlockStackingGameState.PREPARE);
        }

        @Test
        void startPlay_호출_시_PLAYING_상태가_된다() {
            game.prepare();
            game.startPlay();

            assertThat(game.getState()).isEqualTo(BlockStackingGameState.PLAYING);
        }

        @Test
        void finish_호출_시_DONE_상태가_된다() {
            game.prepare();
            game.startPlay();
            game.finish();

            assertThat(game.getState()).isEqualTo(BlockStackingGameState.DONE);
        }
    }

    @Nested
    class 진행_기록_테스트 {

        @BeforeEach
        void 게임_시작() {
            game.prepare();
            game.startPlay();
        }

        @Test
        void 유효한_탭_이벤트_기록_시_true를_반환하고_floor가_증가한다() {
            final boolean recorded = game.recordProgress(Gamer.guest(꾹이),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(recorded).isTrue();
                softly.assertThat(game.getScores().get(GamerFixture.게스트꾹이()).getValue()).isEqualTo(1L);
            });
        }

        @Test
        void 여러_층을_순서대로_쌓을_수_있다() {
            game.recordProgress(Gamer.guest(꾹이),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
            game.recordProgress(Gamer.guest(꾹이),2, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
            game.recordProgress(Gamer.guest(꾹이),3, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);

            assertThat(game.getScores().get(GamerFixture.게스트꾹이()).getValue()).isEqualTo(3L);
        }

        @Test
        void PLAYING_상태가_아닐_때_recordProgress_호출_시_예외를_던진다() {
            game.finish();

            assertCoffeeShoutException(
                    () -> game.recordProgress(Gamer.guest(꾹이),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH),
                    BlockStackingGameErrorCode.NOT_PLAYING_STATE
            );
        }

        @Test
        void 비연속적인_floor_수신_시_false를_반환하고_floor가_갱신되지_않는다() {
            // floor=1을 건너뛰고 floor=2 전송
            final boolean recorded = game.recordProgress(Gamer.guest(꾹이),2, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(recorded).isFalse();
                softly.assertThat(game.getScores().get(GamerFixture.게스트꾹이()).getValue()).isZero();
            });
        }

        @Test
        void overlap이_0인_경우_false를_반환하고_floor가_갱신되지_않는다() {
            // movingBlock이 stackTop과 전혀 겹치지 않는 좌표
            // movingBlockX=300, stackTopX=85, stackTopWidth=150 → stackTop right=235, movingBlock left=300 → overlap<0
            final boolean recorded = game.recordProgress(Gamer.guest(꾹이),1, 300.0, STACK_TOP_X, STACK_TOP_WIDTH);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(recorded).isFalse();
                softly.assertThat(game.getScores().get(GamerFixture.게스트꾹이()).getValue()).isZero();
            });
        }

        @Test
        void overlap이_음수인_경우_false를_반환한다() {
            // movingBlock이 stackTop 좌측으로 완전히 벗어남
            final boolean recorded = game.recordProgress(Gamer.guest(꾹이),1, -100.0, STACK_TOP_X, STACK_TOP_WIDTH);

            assertThat(recorded).isFalse();
        }

        @Test
        void 이미_실패한_플레이어의_진행_이벤트_수신_시_false를_반환하고_floor가_갱신되지_않는다() {
            game.recordProgress(Gamer.guest(꾹이),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
            game.recordFailure(Gamer.guest(꾹이));

            final boolean recorded = game.recordProgress(Gamer.guest(꾹이),2, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(recorded).isFalse();
                softly.assertThat(game.getScores().get(GamerFixture.게스트꾹이()).getValue()).isEqualTo(1L);
            });
        }

        @Test
        void 각_플레이어는_독립적으로_floor를_쌓는다() {
            game.recordProgress(Gamer.guest(꾹이),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
            game.recordProgress(Gamer.guest(꾹이),2, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
            game.recordProgress(Gamer.guest(루키),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(game.getScores().get(GamerFixture.게스트꾹이()).getValue()).isEqualTo(2L);
                softly.assertThat(game.getScores().get(GamerFixture.게스트루키()).getValue()).isEqualTo(1L);
                softly.assertThat(game.getScores().get(GamerFixture.게스트엠제이()).getValue()).isZero();
            });
        }
    }

    @Nested
    class 랭킹_조회_테스트 {

        @BeforeEach
        void 게임_시작() {
            game.prepare();
            game.startPlay();
        }

        @Test
        void 층수_내림차순으로_랭킹을_반환한다() {
            game.recordProgress(Gamer.guest(꾹이),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
            game.recordProgress(Gamer.guest(꾹이),2, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
            game.recordProgress(Gamer.guest(루키),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);

            final List<BlockStackingPlayerRankInfo> ranking = game.getRanking();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(ranking.get(0).name()).isEqualTo("꾹이");
                softly.assertThat(ranking.get(0).floor()).isEqualTo(2);
                softly.assertThat(ranking.get(1).name()).isEqualTo("루키");
                softly.assertThat(ranking.get(1).floor()).isEqualTo(1);
            });
        }

        @Test
        void 초기_상태에서_모든_플레이어_floor가_0이다() {
            final List<BlockStackingPlayerRankInfo> ranking = game.getRanking();

            assertThat(ranking).hasSize(4)
                    .allMatch(r -> r.floor() == 0);
        }
    }

    @Nested
    class 플레이어_조회_테스트 {

        @Test
        void 이름으로_플레이어를_찾는다() {
            final PlayerName found = game.findPlayerByName(new PlayerName("꾹이"));

            assertThat(found).isEqualTo(꾹이);
        }

        @Test
        void 존재하지_않는_플레이어_조회_시_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> game.findPlayerByName(new PlayerName("없는플레이어")),
                    BlockStackingGameErrorCode.PLAYER_NOT_FOUND
            );
        }
    }

    @Nested
    class 실패_기록_테스트 {

        @BeforeEach
        void 게임_시작() {
            game.prepare();
            game.startPlay();
        }

        @Test
        void 플레이어_실패_기록_시_해당_플레이어의_failed가_true가_된다() {
            final boolean recorded = game.recordFailure(Gamer.guest(꾹이));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(recorded).isTrue();
                softly.assertThat(game.getPlayerProgresses().get(꾹이).failed()).isTrue();
            });
        }

        @Test
        void 실패_기록_시_쌓은_층수는_유지된다() {
            game.recordProgress(Gamer.guest(꾹이),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
            game.recordProgress(Gamer.guest(꾹이),2, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);

            game.recordFailure(Gamer.guest(꾹이));

            assertThat(game.getScores().get(GamerFixture.게스트꾹이()).getValue()).isEqualTo(2L);
        }

        @Test
        void 이미_실패한_플레이어에게_중복_실패_기록_시_false를_반환하고_상태가_유지된다() {
            game.recordFailure(Gamer.guest(꾹이));
            final boolean second = game.recordFailure(Gamer.guest(꾹이));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(second).isFalse();
                softly.assertThat(game.getPlayerProgresses().get(꾹이).failed()).isTrue();
            });
        }

        @Test
        void PLAYING_상태가_아닐_때_recordFailure_호출_시_예외를_던진다() {
            game.finish();

            assertCoffeeShoutException(
                    () -> game.recordFailure(Gamer.guest(꾹이)),
                    BlockStackingGameErrorCode.NOT_PLAYING_STATE
            );
        }

        @Test
        void 등록되지_않은_플레이어의_실패_기록_시_예외를_던진다() {
            final PlayerName 미등록플레이어 = new PlayerName("유령");

            assertCoffeeShoutException(
                    () -> game.recordFailure(Gamer.guest(미등록플레이어)),
                    GamerErrorCode.UNAUTHORIZED_GAMER
            );
        }
    }

    @Nested
    class 전원_실패_여부_테스트 {

        @BeforeEach
        void 게임_시작() {
            game.prepare();
            game.startPlay();
        }

        @Test
        void 아무도_실패하지_않은_경우_false를_반환한다() {
            assertThat(game.isAllPlayersFailed()).isFalse();
        }

        @Test
        void 일부만_실패한_경우_false를_반환한다() {
            game.recordFailure(Gamer.guest(꾹이));
            game.recordFailure(Gamer.guest(루키));

            assertThat(game.isAllPlayersFailed()).isFalse();
        }

        @Test
        void 모든_플레이어가_실패하면_true를_반환한다() {
            game.recordFailure(Gamer.guest(꾹이));
            game.recordFailure(Gamer.guest(루키));
            game.recordFailure(Gamer.guest(엠제이));
            game.recordFailure(Gamer.guest(한스));

            assertThat(game.isAllPlayersFailed()).isTrue();
        }
    }

    @Nested
    class 점수_결과_테스트 {

        @BeforeEach
        void 게임_시작_및_층수_쌓기() {
            game.prepare();
            game.startPlay();

            game.recordProgress(Gamer.guest(꾹이),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
            game.recordProgress(Gamer.guest(꾹이),2, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
            game.recordProgress(Gamer.guest(루키),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
        }

        @Test
        void getScores_가_각_플레이어의_현재_층수를_반환한다() {
            final Map<Gamer, MiniGameScore> scores = game.getScores();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(scores.get(GamerFixture.게스트꾹이()).getValue()).isEqualTo(2L);
                softly.assertThat(scores.get(GamerFixture.게스트루키()).getValue()).isEqualTo(1L);
                softly.assertThat(scores.get(GamerFixture.게스트엠제이()).getValue()).isZero();
                softly.assertThat(scores.get(GamerFixture.게스트한스()).getValue()).isZero();
            });
        }

        @Test
        void getResult_가_층수_내림차순으로_순위를_매긴다() {
            final var result = game.getResult();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getPlayerRank(GamerFixture.게스트꾹이())).isEqualTo(1);
                softly.assertThat(result.getPlayerRank(GamerFixture.게스트루키())).isEqualTo(2);
            });
        }

        @Test
        void 층수가_같은_플레이어는_동일_순위를_받는다() {
            game.recordProgress(Gamer.guest(엠제이),1, MOVING_BLOCK_X, STACK_TOP_X, STACK_TOP_WIDTH);
            // 루키=1층, 엠제이=1층 → 공동 2위

            final var result = game.getResult();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getPlayerRank(GamerFixture.게스트루키())).isEqualTo(2);
                softly.assertThat(result.getPlayerRank(GamerFixture.게스트엠제이())).isEqualTo(2);
            });
        }
    }
}
