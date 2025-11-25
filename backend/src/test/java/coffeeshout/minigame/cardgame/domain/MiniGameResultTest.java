package coffeeshout.minigame.cardgame.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.fixture.PlayerFixture;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.racinggame.domain.RacingGameScore;
import coffeeshout.room.domain.player.Player;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class MiniGameResultTest {

    private final Player 호스트_한스 = PlayerFixture.호스트한스();
    private final Player 게스트_루키 = PlayerFixture.호스트루키();
    private final Player 게스트_꾹이 = PlayerFixture.호스트꾹이();
    private final Player 게스트_엠제이 = PlayerFixture.호스트엠제이();

    @Test
    void 순위_목록을_반환한다() {
        // given
        Map<Player, MiniGameScore> playerScores = Map.of(
                게스트_엠제이, new CardGameScore(80),
                호스트_한스, new CardGameScore(40),
                게스트_루키, new CardGameScore(10),
                게스트_꾹이, new CardGameScore(-40)
        );

        // when
        MiniGameResult miniGameResult = MiniGameResult.fromDescending(playerScores);

        // then
        SoftAssertions.assertSoftly(
                softly -> {
                    softly.assertThat(miniGameResult.getRank().size()).isEqualTo(4);
                    softly.assertThat(miniGameResult.getRank()).containsExactlyInAnyOrderEntriesOf(
                            Map.of(
                                    게스트_엠제이, 1,
                                    호스트_한스, 2,
                                    게스트_루키, 3,
                                    게스트_꾹이, 4
                            )
                    );
                }
        );
    }

    @Test
    void 해당_플레이어의_순위를_반환한다() {
        // given
        Map<Player, MiniGameScore> playerScores = Map.of(
                게스트_엠제이, new CardGameScore(80),
                호스트_한스, new CardGameScore(40),
                게스트_루키, new CardGameScore(10),
                게스트_꾹이, new CardGameScore(-40)
        );

        // when
        MiniGameResult miniGameResult = MiniGameResult.fromDescending(playerScores);

        // then
        SoftAssertions.assertSoftly(
                softly -> {
                    softly.assertThat(miniGameResult.getPlayerRank(게스트_엠제이)).isEqualTo(1);
                    softly.assertThat(miniGameResult.getPlayerRank(호스트_한스)).isEqualTo(2);
                    softly.assertThat(miniGameResult.getPlayerRank(게스트_루키)).isEqualTo(3);
                    softly.assertThat(miniGameResult.getPlayerRank(게스트_꾹이)).isEqualTo(4);
                }
        );
    }

    @Test
    void 동점자가_몇_명인지_확인한다() {
        // given
        MiniGameResult result = MiniGameResult.fromDescending(Map.of(
                호스트_한스, new RacingGameScore(99999),
                게스트_루키, new RacingGameScore(99999),
                게스트_엠제이, new RacingGameScore(99999),
                게스트_꾹이, new RacingGameScore(99999)
        ));

        // when
        int count = result.getTieCountByRank(1);

        // then
        assertThat(count).isEqualTo(4);
    }
}
