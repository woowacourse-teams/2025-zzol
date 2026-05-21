package coffeeshout.racinggame.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.fixture.GamerFixture;
import coffeeshout.fixture.PlayerFixture;
import coffeeshout.exception.custom.BusinessException;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RacingGameTest {

    final RacingGame racingGame = new RacingGame();
    final Player 한스 = PlayerFixture.호스트한스();
    final Player 꾹이 = PlayerFixture.게스트꾹이();
    final List<PlayerName> players = List.of(한스.getName(), 꾹이.getName());

    @Test
    void 게임_시작을_위해_준비한다() {
        // when
        racingGame.setUp(players.stream().map(name -> new Gamer(name, null)).toList());

        // then
        assertThat(racingGame.getState()).isEqualTo(RacingGameState.DESCRIPTION);
        assertThat(racingGame.getPositions()).hasSize(2);
    }

    @Test
    void 모든_러너를_이동시킬_수_있다() {
        // given
        racingGame.setUp(players.stream().map(name -> new Gamer(name, null)).toList());
        racingGame.updateState(RacingGameState.PLAYING);

        racingGame.updateSpeed(Gamer.guest(players.getFirst()),10, (lastTapedTime, now, tapCount) -> 10, Instant.now());
        racingGame.updateSpeed(Gamer.guest(players.get(1)),10, (lastTapedTime, now, tapCount) -> 10, Instant.now());

        // when
        racingGame.moveAll();

        // then
        final Map<Runner, Integer> positions = racingGame.getPositions();
        assertThat(positions.values()).allMatch(position -> position == 10);
    }

    @Test
    void 모든_러너가_결승선에_도착한다() {
        // given
        racingGame.setUp(players.stream().map(name -> new Gamer(name, null)).toList());
        racingGame.updateState(RacingGameState.PLAYING);

        racingGame.updateSpeed(Gamer.guest(players.getFirst()),10, (lastTapedTime, now, tapCount) -> 30, Instant.now());
        racingGame.updateSpeed(Gamer.guest(players.get(1)),10, (lastTapedTime, now, tapCount) -> 30, Instant.now());

        // when
        for (int i = 0; i < 101; ++i) {
            racingGame.moveAll();
        }

        // then
        assertThat(racingGame.isFinished()).isTrue();
    }

    @Test
    void 러너의_속도를_조절한다() {
        // given
        racingGame.setUp(players.stream().map(name -> new Gamer(name, null)).toList());
        racingGame.updateState(RacingGameState.PLAYING);

        racingGame.updateSpeed(Gamer.guest(players.getFirst()),10, (lastTapedTime, now, tapCount) -> 10, Instant.now());
        racingGame.updateSpeed(Gamer.guest(players.get(1)),10, (lastTapedTime, now, tapCount) -> 10, Instant.now());

        // then
        assertThat(racingGame.getRunners().getSpeeds().values()).allMatch(value -> value == 10);
    }

    @Test
    void 게임이_진행_중이_아니면_속도_조정시_예외가_발생한다() {
        // given
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> 10;
        racingGame.setUp(players.stream().map(name -> new Gamer(name, null)).toList());

        // when && then
        assertThatThrownBy(() -> racingGame.updateSpeed(Gamer.guest(players.getFirst()),10, speedCalculator, Instant.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 게임_결과를_조회할_수_있다() throws InterruptedException {
        // given
        racingGame.setUp(players.stream().map(name -> new Gamer(name, null)).toList());
        racingGame.updateState(RacingGameState.PLAYING);
        racingGame.setUpStart();
        racingGame.setAutoMoveFuture(null);

        for (int i = 0; i < 100; i++) {
            racingGame.updateSpeed(Gamer.guest(players.get(1)),10, (lastTapedTime, now, tapCount) -> 30, Instant.now());
            racingGame.updateSpeed(Gamer.guest(players.getFirst()),10, (lastTapedTime, now, tapCount) -> 10, Instant.now());
            racingGame.moveAll();
        }

        Thread.sleep(2);

        for (int i = 0; i < 200; i++) {
            racingGame.updateSpeed(Gamer.guest(players.getFirst()),10, (lastTapedTime, now, tapCount) -> 10, Instant.now());
            racingGame.moveAll();
        }

        // when
        final var result = racingGame.getResult();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRank().get(Gamer.guest(players.getFirst()))).isEqualTo(2);
        assertThat(result.getRank().get(Gamer.guest(players.get(1)))).isEqualTo(1);
    }
}
