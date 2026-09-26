package coffeeshout.laddergame.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.laddergame.application.LadderCommandService;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.laddergame.domain.LadderGameState;
import coffeeshout.laddergame.domain.LadderLine;
import coffeeshout.room.domain.player.Player;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LadderCommandServiceTest {

    LadderCommandService commandService;
    LadderGame game;
    Player 꾹이;
    Player 루키;
    Player 엠제이;

    @BeforeEach
    void setUp() {
        commandService = new LadderCommandService();
        꾹이 = PlayerFixture.호스트꾹이();
        루키 = PlayerFixture.게스트루키();
        엠제이 = PlayerFixture.게스트엠제이();

        game = new LadderGame();
        game.setUp(List.of(꾹이.toGamer(), 루키.toGamer(), 엠제이.toGamer()));
        game.changeToPrepare();
        game.changeToDrawing();
    }

    @Nested
    class 정상_선_긋기 {

        @Test
        void 유효한_요청은_LadderLine을_반환한다() {
            final Optional<LadderLine> result = commandService.drawLine(game, "꾹이", 0, 1);

            assertThat(result).isPresent();
        }

        @Test
        void 반환된_선의_playerName_segmentIndex_row가_요청과_일치한다() {
            final LadderLine line = commandService.drawLine(game, "꾹이", 1, 5).orElseThrow();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(line.playerName()).isEqualTo("꾹이");
                softly.assertThat(line.segmentIndex()).isEqualTo(1);
                softly.assertThat(line.row()).isEqualTo(5);
            });
        }

        @Test
        void 한_플레이어가_선을_2개까지_그을_수_있다() {
            commandService.drawLine(game, "꾹이", 0, 1);

            final Optional<LadderLine> second = commandService.drawLine(game, "꾹이", 0, 2);

            assertThat(second).isPresent();
        }
    }

    @Nested
    class 검증_실패_무시 {

        @Test
        void DRAWING_상태가_아니면_빈_Optional을_반환한다() {
            game.changeToResult();

            final Optional<LadderLine> result = commandService.drawLine(game, "꾹이", 0, 1);

            assertThat(result).isEmpty();
        }

        @Test
        void DESCRIPTION_상태에서도_빈_Optional을_반환한다() {
            final LadderGame descriptionGame = new LadderGame();
            descriptionGame.setUp(List.of(꾹이.toGamer(), 루키.toGamer(), 엠제이.toGamer()));

            final Optional<LadderLine> result = commandService.drawLine(descriptionGame, "꾹이", 0, 1);

            assertThat(result).isEmpty();
        }

        @Test
        void 미참여자_요청은_빈_Optional을_반환한다() {
            final Optional<LadderLine> result = commandService.drawLine(game, "없는플레이어", 0, 1);

            assertThat(result).isEmpty();
        }

        @Test
        void 선을_2개_그은_플레이어의_세_번째_요청은_빈_Optional을_반환한다() {
            commandService.drawLine(game, "꾹이", 0, 1);
            commandService.drawLine(game, "꾹이", 0, 2);

            final Optional<LadderLine> third = commandService.drawLine(game, "꾹이", 0, 3);

            assertThat(third).isEmpty();
        }

        @Test
        void 이미_선이_있는_자리_요청은_빈_Optional을_반환한다() {
            commandService.drawLine(game, "꾹이", 0, 5);

            final Optional<LadderLine> result = commandService.drawLine(game, "루키", 1, 5);

            assertThat(result).isEmpty();
        }

        @Test
        void 높이_칸_수를_넘는_row는_빈_Optional을_반환한다() {
            // 기둥 3개 → 높이 칸 6개 → 7은 유효하지 않음
            final Optional<LadderLine> result = commandService.drawLine(game, "꾹이", 0, 7);

            assertThat(result).isEmpty();
        }

        @Test
        void 높이_0은_빈_Optional을_반환한다() {
            final Optional<LadderLine> result = commandService.drawLine(game, "꾹이", 0, 0);

            assertThat(result).isEmpty();
        }

        @Test
        void 유효하지_않은_segmentIndex는_빈_Optional을_반환한다() {
            // 기둥 3개 → 유효한 구간: 0, 1 → 2는 유효하지 않음
            final Optional<LadderLine> result = commandService.drawLine(game, "꾹이", 2, 1);

            assertThat(result).isEmpty();
        }

        @Test
        void 음수_segmentIndex는_빈_Optional을_반환한다() {
            final Optional<LadderLine> result = commandService.drawLine(game, "꾹이", -1, 1);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class 상태_유지_검증 {

        @Test
        void 검증_실패_후_다른_플레이어는_정상적으로_선을_그을_수_있다() {
            commandService.drawLine(game, "없는플레이어", 0, 1); // 무시됨

            final Optional<LadderLine> result = commandService.drawLine(game, "꾹이", 0, 1);

            assertThat(result).isPresent();
        }

        @Test
        void 검증_실패는_게임_상태를_변경하지_않는다() {
            commandService.drawLine(game, "꾹이", -1, 1); // 무시됨

            assertThat(game.getState()).isEqualTo(LadderGameState.DRAWING);
        }
    }
}
