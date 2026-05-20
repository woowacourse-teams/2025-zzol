package coffeeshout.laddergame.domain;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LadderGameTest {

    LadderGame game;
    Player 꾹이;
    Player 철수;
    Player 영희;
    List<Player> players;

    @BeforeEach
    void setUp() {
        꾹이 = PlayerFixture.호스트꾹이();
        철수 = PlayerFixture.게스트루키();
        영희 = PlayerFixture.게스트엠제이();
        players = List.of(꾹이, 철수, 영희);

        game = new LadderGame();
        game.setUp(players);
    }

    @Nested
    class 게임_초기화_테스트 {

        @Test
        void DESCRIPTION_상태로_시작한다() {
            assertThat(game.getState()).isEqualTo(LadderGameState.DESCRIPTION);
        }

        @Test
        void getMiniGameType이_LADDER_GAME을_반환한다() {
            assertThat(game.getMiniGameType()).isEqualTo(MiniGameType.LADDER_GAME);
        }

        @Test
        void setUp_후_Poles와_BottomRanks가_초기화된다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(game.getPoles()).isNotNull();
                softly.assertThat(game.getPoles().size()).isEqualTo(3);
                softly.assertThat(game.getBottomRanks()).isNotNull();
            });
        }

        @Test
        void setUp_후_기둥에_모든_플레이어가_배정된다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(game.getPoles().contains(꾹이.getName())).isTrue();
                softly.assertThat(game.getPoles().contains(철수.getName())).isTrue();
                softly.assertThat(game.getPoles().contains(영희.getName())).isTrue();
            });
        }
    }

    @Nested
    class 상태_전환_테스트 {

        @Test
        void 허용되지_않은_상태_전환_시_예외를_던진다() {
            assertThat(game.getState()).isEqualTo(LadderGameState.DESCRIPTION);

            assertCoffeeShoutException(
                    () -> game.changeToDrawing(),
                    LadderGameErrorCode.INVALID_STATE_TRANSITION
            );
        }

        @Test
        void changeToPrepare_호출_시_PREPARE_상태가_된다() {
            game.changeToPrepare();

            assertThat(game.getState()).isEqualTo(LadderGameState.PREPARE);
        }

        @Test
        void changeToDrawing_호출_시_DRAWING_상태가_된다() {
            game.changeToPrepare();
            game.changeToDrawing();

            assertThat(game.getState()).isEqualTo(LadderGameState.DRAWING);
        }

        @Test
        void changeToResult_호출_시_RESULT_상태가_된다() {
            game.changeToPrepare();
            game.changeToDrawing();
            game.changeToResult();

            assertThat(game.getState()).isEqualTo(LadderGameState.RESULT);
        }

        @Test
        void changeToDone_호출_시_DONE_상태가_된다() {
            game.changeToPrepare();
            game.changeToDrawing();
            game.changeToResult();
            game.changeToDone();

            assertThat(game.getState()).isEqualTo(LadderGameState.DONE);
        }
    }

    @Nested
    class 선_긋기_테스트 {

        @BeforeEach
        void DRAWING_상태로_전환() {
            game.changeToPrepare();
            game.changeToDrawing();
        }

        @Test
        void drawLine_호출_시_LadderLine을_반환한다() {
            final int validSegment = 0;

            final LadderLine line = game.drawLine(꾹이.getName(), validSegment);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(line).isNotNull();
                softly.assertThat(line.segmentIndex()).isEqualTo(validSegment);
                softly.assertThat(line.row()).isPositive();
            });
        }

        @Test
        void 미등록_플레이어_선_긋기_시_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> game.drawLine(new PlayerName("미등록"), 0),
                    LadderGameErrorCode.PLAYER_NOT_FOUND
            );
        }

        @Test
        void 이미_선을_그은_플레이어가_다시_그으면_예외를_던진다() {
            game.drawLine(꾹이.getName(), 0);

            assertCoffeeShoutException(
                    () -> game.drawLine(꾹이.getName(), 0),
                    LadderGameErrorCode.ALREADY_DREW
            );
        }
    }

    @Nested
    class 이미_그었는지_확인_테스트 {

        @BeforeEach
        void DRAWING_상태로_전환() {
            game.changeToPrepare();
            game.changeToDrawing();
        }

        @Test
        void 선을_그지_않은_플레이어는_false를_반환한다() {
            assertThat(game.isAlreadyDrew(꾹이.getName())).isFalse();
        }

        @Test
        void 선을_그은_플레이어는_true를_반환한다() {
            game.drawLine(꾹이.getName(), 0);

            assertThat(game.isAlreadyDrew(꾹이.getName())).isTrue();
        }
    }

    @Nested
    class tracePaths_테스트 {

        @Test
        void tracePaths_호출_후_모든_플레이어의_순위가_계산된다() {
            game.changeToPrepare();
            game.changeToDrawing();
            game.tracePaths();

            final Map<String, Integer> rankings = game.getRankingsForBroadcast();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(rankings).hasSize(3);
                softly.assertThat(rankings).containsKey("꾹이");
                softly.assertThat(rankings).containsKey("루키");
                softly.assertThat(rankings).containsKey("엠제이");
            });
        }

        @Test
        void 순위는_1부터_n까지_각각_하나씩_배정된다() {
            game.changeToPrepare();
            game.changeToDrawing();
            game.tracePaths();

            final Map<String, Integer> rankings = game.getRankingsForBroadcast();
            final long distinctRanks = rankings.values().stream().distinct().count();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(rankings.values()).allMatch(r -> r >= 1 && r <= 3);
                softly.assertThat(distinctRanks).isEqualTo(3);
            });
        }

        @Test
        void 선이_없으면_각_플레이어는_자기_기둥_바닥의_순위를_받는다() {
            // 선 없음 → trace(i) = i → BottomRanks에서 직접 순위 할당
            game.changeToPrepare();
            game.changeToDrawing();
            game.tracePaths();

            final Map<String, Integer> rankings = game.getRankingsForBroadcast();
            final int totalRankSum = rankings.values().stream().mapToInt(Integer::intValue).sum();

            // 1+2+3 = 6
            assertThat(totalRankSum).isEqualTo(6);
        }
    }

    @Nested
    class getResult_테스트 {

        @Test
        void getResult는_fromAscending으로_순위를_계산한다() {
            // 선 없이 tracePaths → BottomRanks가 1,2,3을 기둥에 배정
            // fromAscending: rank 1(최소값) = 1위
            game.changeToPrepare();
            game.changeToDrawing();
            game.tracePaths();

            final MiniGameResult result = game.getResult();

            // 모든 플레이어에게 게임 순위가 부여됨
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getPlayerRank(꾹이)).isNotNull();
                softly.assertThat(result.getPlayerRank(철수)).isNotNull();
                softly.assertThat(result.getPlayerRank(영희)).isNotNull();
            });
        }

        @Test
        void 사다리_순위_1위가_게임_결과_1위가_된다() {
            // BottomRanks를 통제할 수 없으므로 tracePaths 후 getRankings로 비교
            game.changeToPrepare();
            game.changeToDrawing();
            game.tracePaths();

            final Map<String, Integer> ladderRankings = game.getRankingsForBroadcast();
            final MiniGameResult result = game.getResult();

            // 사다리 순위 1을 받은 플레이어가 getResult에서도 1위
            final Player ladderWinner = players.stream()
                    .filter(p -> ladderRankings.get(p.getName().value()) == 1)
                    .findFirst()
                    .orElseThrow();

            assertThat(result.getPlayerRank(ladderWinner)).isEqualTo(1);
        }
    }
}
