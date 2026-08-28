package coffeeshout.racinggame.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.gamecommon.Gamer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RunnersTest {

    private final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> 30;

    final List<Gamer> players =
            List.of(PlayerFixture.호스트한스().toGamer(), PlayerFixture.게스트꾹이().toGamer());
    final Runners runners = new Runners(players);

    @Test
    void 플레이어의_속도를_업데이트할_수_있다() {
        // when
        runners.updateSpeed(players.getFirst().getName(), 5, speedCalculator, Instant.now());

        // then
        assertThat(runners.getRunners().getFirst().getSpeed()).isEqualTo(30);
    }

    @Test
    void 모든_러너를_이동시킬_수_있다() {
        // when
        runners.moveAll(Instant.now());

        // then
        final Map<Runner, Integer> positions = runners.getPositions();
        assertThat(positions.values()).allMatch(position -> position == RacingGame.START_LINE);
    }

    @Test
    void 우승자를_찾을_수_있다() {
        // given
        final Instant now = Instant.now();
        달린다(100, now, players.getFirst().getName());

        // when
        final Runner winner = runners.findWinner().get();

        // then
        assertThat(winner.getGamer()).isEqualTo(players.getFirst());
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
        달린다(100, now, players.getFirst().getName());

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
        assertThat(positions.values()).allMatch(position -> position == RacingGame.START_LINE);
    }

    @Test
    void 모든_러너의_속도를_조회할_수_있다() {
        // given
        runners.updateSpeed(players.getFirst().getName(), 8, speedCalculator, Instant.now());
        runners.updateSpeed(players.get(1).getName(), 8, speedCalculator, Instant.now());

        // then
        assertThat(runners.getSpeeds().values()).allMatch(speed -> speed == 30);
    }

    @Test
    void 모든_러너가_완주했는지_확인할_수_있다() {
        // given
        final Instant now = Instant.now();
        달린다(100, now, players.getFirst().getName(), players.get(1).getName());

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
        assertThat(runners.getRunners().getFirst().getLastSpeedUpdateTime()).isEqualTo(time);
    }

    /**
     * 지정한 러너들이 매 틱 탭하며 달린다. 주행 중 감속이 있으므로 속도를 한 번만 주고
     * 반복 이동하면 속도가 계속 줄어 완주하지 못한다.
     */
    private void 달린다(int ticks, Instant now, String... tappingNames) {
        for (int i = 0; i < ticks; i++) {
            for (final String name : tappingNames) {
                runners.updateSpeed(name, 10, speedCalculator, now);
            }
            runners.moveAll(now);
        }
    }
}
