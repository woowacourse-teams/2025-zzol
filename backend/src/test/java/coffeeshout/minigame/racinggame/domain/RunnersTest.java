package coffeeshout.minigame.racinggame.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.Runner;
import coffeeshout.racinggame.domain.Runners;
import coffeeshout.racinggame.domain.SpeedCalculator;
import coffeeshout.room.domain.player.Player;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RunnersTest {

    private final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> 30;

    final List<Player> players = List.of(PlayerFixture.호스트한스(), PlayerFixture.게스트꾹이());
    final Runners runners = new Runners(players);

    @Test
    void 플레이어의_속도를_업데이트할_수_있다() {
        // when
        runners.updateSpeed(players.get(0), 5, speedCalculator, Instant.now());

        // then
        assertThat(runners.getRunners().get(0).getSpeed()).isEqualTo(30);
    }

    @Test
    void 모든_러너를_이동시킬_수_있다() {
        // when
        runners.moveAll(Instant.now());

        // then
        final Map<Runner, Integer> positions = runners.getPositions();
        assertThat(positions.values()).allMatch(position -> position == RacingGame.INITIAL_SPEED);
    }

    @Test
    void 우승자를_찾을_수_있다() {
        // given
        final Instant now = Instant.now();
        runners.updateSpeed(players.get(0), 10, speedCalculator, now);
        for (int i = 0; i < 100; i++) {
            runners.moveAll(now);
        }

        // when
        final Runner winner = runners.findWinner().get();

        // then
        assertThat(winner.getPlayer()).isEqualTo(players.get(0));
    }

    @Test
    void 우승자가_없으면_빈_Optional을_반환한다() {
        // when && then
        assertThat(runners.findWinner()).isEmpty();
    }

    @Test
    void 우승자가_있는지_확인할_수_있다() {
        // given
        final Instant now = Instant.now();
        runners.updateSpeed(players.get(0), 10, speedCalculator, now);
        for (int i = 0; i < 100; i++) {
            runners.moveAll(now);
        }

        // when && then
        assertThat(runners.hasWinner()).isTrue();
    }

    @Test
    void 모든_러너의_위치를_조회할_수_있다() {
        // given
        runners.moveAll(Instant.now());

        // when
        final Map<Runner, Integer> positions = runners.getPositions();

        // then
        assertThat(positions).hasSize(2);
        assertThat(positions.values()).allMatch(position -> position == RacingGame.INITIAL_SPEED);
    }

    @Test
    void 모든_러너의_속도를_조회할_수_있다() {
        // given
        runners.updateSpeed(players.get(0), 8, speedCalculator, Instant.now());
        runners.updateSpeed(players.get(1), 8, speedCalculator, Instant.now());

        // then
        assertThat(runners.getSpeeds().values()).allMatch(speed -> speed == 30);
    }

    @Test
    void 모든_러너가_완주했는지_확인할_수_있다() {
        // given
        final Instant now = Instant.now();
        runners.updateSpeed(players.get(0), 10, speedCalculator, now);
        runners.updateSpeed(players.get(1), 10, speedCalculator, now);
        for (int i = 0; i < 100; i++) {
            runners.moveAll(now);
        }

        // when && then
        assertThat(runners.isAllFinished()).isTrue();
    }

    @Test
    void 모든_러너가_완주하지_않았는지_확인할_수_있다() {
        // when && then
        assertThat(runners.isAllFinished()).isFalse();
    }

    @Test
    void 초기_속도를_설정할_수_있다() {
        // when
        runners.initialSpeed();

        // then
        assertThat(runners.getSpeeds().values()).allMatch(speed -> speed == 3);
    }

    @Test
    void 초기_탭_시간을_설정할_수_있다() {
        // given
        final Instant time = Instant.now();

        // when
        runners.initialLastTapTime(time);

        // then
        assertThat(runners.getRunners().get(0).getLastSpeedUpdateTime()).isEqualTo(time);
    }
}
