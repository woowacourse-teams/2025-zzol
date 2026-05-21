package coffeeshout.laddergame.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.laddergame.domain.LadderGameState;
import coffeeshout.laddergame.domain.LadderLine;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LadderCommandServiceTest {

    LadderCommandService commandService;
    LadderGame game;
    Player 꾹이;
    Player 루키;
    Player 엠제이;
    Gamer gamer꾹이;
    Gamer gamer없는플레이어;

    @BeforeEach
    void setUp() {
        commandService = new LadderCommandService();
        꾹이 = PlayerFixture.호스트꾹이();
        루키 = PlayerFixture.게스트루키();
        엠제이 = PlayerFixture.게스트엠제이();
        gamer꾹이 = new Gamer(꾹이.getName(), null);
        gamer없는플레이어 = Gamer.guest(new PlayerName("없는플레이어"));

        game = new LadderGame();
        game.setUp(List.of(gamer꾹이, new Gamer(루키.getName(), null), new Gamer(엠제이.getName(), null)));
        game.changeToPrepare();
        game.changeToDrawing();
    }

    @Nested
    class 정상_선_긋기 {

        @Test
        void 유효한_요청은_LadderLine을_반환한다() {
            final Optional<LadderLine> result = commandService.drawLine(game, gamer꾹이, 0);

            assertThat(result).isPresent();
        }

        @Test
        void 반환된_선의_playerName과_segmentIndex가_요청과_일치한다() {
            final LadderLine line = commandService.drawLine(game, gamer꾹이, 1).orElseThrow();

            assertThat(line.segmentIndex()).isEqualTo(1);
            assertThat(line.playerName().value()).isEqualTo("꾹이");
        }
    }

    @Nested
    class 검증_실패_무시 {

        @Test
        void DRAWING_상태가_아니면_빈_Optional을_반환한다() {
            game.changeToResult();

            final Optional<LadderLine> result = commandService.drawLine(game, gamer꾹이, 0);

            assertThat(result).isEmpty();
        }

        @Test
        void DESCRIPTION_상태에서도_빈_Optional을_반환한다() {
            final LadderGame descriptionGame = new LadderGame();
            descriptionGame.setUp(List.of(gamer꾹이, new Gamer(루키.getName(), null), new Gamer(엠제이.getName(), null)));

            final Optional<LadderLine> result = commandService.drawLine(descriptionGame, gamer꾹이, 0);

            assertThat(result).isEmpty();
        }

        @Test
        void 미참여자_요청은_빈_Optional을_반환한다() {
            final Optional<LadderLine> result = commandService.drawLine(game, gamer없는플레이어, 0);

            assertThat(result).isEmpty();
        }

        @Test
        void 이미_선을_그은_플레이어_재요청은_빈_Optional을_반환한다() {
            commandService.drawLine(game, gamer꾹이, 0);

            final Optional<LadderLine> second = commandService.drawLine(game, gamer꾹이, 1);

            assertThat(second).isEmpty();
        }

        @Test
        void 유효하지_않은_segmentIndex는_빈_Optional을_반환한다() {
            // 기둥 3개 → 유효한 구간: 0, 1 → 2는 유효하지 않음
            final Optional<LadderLine> result = commandService.drawLine(game, gamer꾹이, 2);

            assertThat(result).isEmpty();
        }

        @Test
        void 음수_segmentIndex는_빈_Optional을_반환한다() {
            final Optional<LadderLine> result = commandService.drawLine(game, gamer꾹이, -1);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class 상태_유지_검증 {

        @Test
        void 검증_실패_후_다른_플레이어는_정상적으로_선을_그을_수_있다() {
            commandService.drawLine(game, gamer없는플레이어, 0); // 무시됨

            final Optional<LadderLine> result = commandService.drawLine(game, gamer꾹이, 0);

            assertThat(result).isPresent();
        }

        @Test
        void 검증_실패는_게임_상태를_변경하지_않는다() {
            commandService.drawLine(game, gamer꾹이, -1); // 무시됨

            assertThat(game.getState()).isEqualTo(LadderGameState.DRAWING);
        }
    }
}
