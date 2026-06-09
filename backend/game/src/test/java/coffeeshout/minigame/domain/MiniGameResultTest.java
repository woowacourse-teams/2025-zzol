package coffeeshout.minigame.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.cardgame.domain.CardGameScore;
import coffeeshout.fixture.PlayerFixture;
import coffeeshout.racinggame.domain.RacingGameScore;
import coffeeshout.gamecommon.Gamer;
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
        Map<Gamer, MiniGameScore> playerScores = Map.of(
                게스트_엠제이.toGamer(), new CardGameScore(80),
                호스트_한스.toGamer(), new CardGameScore(40),
                게스트_루키.toGamer(), new CardGameScore(10),
                게스트_꾹이.toGamer(), new CardGameScore(-40)
        );

        // when
        MiniGameResult miniGameResult = MiniGameResult.fromDescending(playerScores);

        // then
        SoftAssertions.assertSoftly(
                softly -> {
                    softly.assertThat(miniGameResult.getRank().size()).isEqualTo(4);
                    softly.assertThat(miniGameResult.getRank()).containsExactlyInAnyOrderEntriesOf(
                            Map.of(
                                    게스트_엠제이.toGamer(), 1,
                                    호스트_한스.toGamer(), 2,
                                    게스트_루키.toGamer(), 3,
                                    게스트_꾹이.toGamer(), 4
                            )
                    );
                }
        );
    }

    @Test
    void 해당_플레이어의_순위를_반환한다() {
        // given
        Map<Gamer, MiniGameScore> playerScores = Map.of(
                게스트_엠제이.toGamer(), new CardGameScore(80),
                호스트_한스.toGamer(), new CardGameScore(40),
                게스트_루키.toGamer(), new CardGameScore(10),
                게스트_꾹이.toGamer(), new CardGameScore(-40)
        );

        // when
        MiniGameResult miniGameResult = MiniGameResult.fromDescending(playerScores);

        // then
        SoftAssertions.assertSoftly(
                softly -> {
                    softly.assertThat(miniGameResult.getPlayerRank(게스트_엠제이.toGamer())).isEqualTo(1);
                    softly.assertThat(miniGameResult.getPlayerRank(호스트_한스.toGamer())).isEqualTo(2);
                    softly.assertThat(miniGameResult.getPlayerRank(게스트_루키.toGamer())).isEqualTo(3);
                    softly.assertThat(miniGameResult.getPlayerRank(게스트_꾹이.toGamer())).isEqualTo(4);
                }
        );
    }

    @Test
    void toRankMap은_순위를_플레이어_이름_기준으로_변환한다() {
        // given
        MiniGameResult result = new MiniGameResult(Map.of(
                호스트_한스.toGamer(), 1,
                게스트_루키.toGamer(), 2,
                게스트_꾹이.toGamer(), 3
        ));

        // when
        Map<String, Integer> rankMap = result.toRankMap();

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(rankMap).containsExactlyInAnyOrderEntriesOf(Map.of(
                    호스트_한스.toGamer().getName(), 1,
                    게스트_루키.toGamer().getName(), 2,
                    게스트_꾹이.toGamer().getName(), 3
            ));
        });
    }

    @Test
    void 동점자가_몇_명인지_확인한다() {
        // given
        MiniGameResult result = MiniGameResult.fromDescending(Map.of(
                호스트_한스.toGamer(), new RacingGameScore(99999),
                게스트_루키.toGamer(), new RacingGameScore(99999),
                게스트_엠제이.toGamer(), new RacingGameScore(99999),
                게스트_꾹이.toGamer(), new RacingGameScore(99999)
        ));

        // when
        int count = result.getTieCountByRank(1);

        // then
        assertThat(count).isEqualTo(4);
    }
}
