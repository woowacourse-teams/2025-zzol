package coffeeshout.racinggame.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.player.Player;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class RacingGameTest {

    final RacingGame racingGame = new RacingGame();
    final List<Player> players = List.of(PlayerFixture.호스트한스(), PlayerFixture.게스트꾹이());

    @Test
    void 게임_시작을_위해_준비한다() {
        // when
        racingGame.setUp(players.stream().map(p -> p.toGamer()).toList());

        // then
        assertThat(racingGame.getState()).isEqualTo(RacingGameState.DESCRIPTION);
        assertThat(racingGame.getPositions()).hasSize(2);
    }

    @Test
    void 모든_러너를_이동시킬_수_있다() {
        // given
        racingGame.setUp(players.stream().map(p -> p.toGamer()).toList());
        racingGame.updateState(RacingGameState.PLAYING);

        racingGame.updateSpeed(
                players.getFirst().getName().value(), 10, (lastTapedTime, now, tapCount) -> 10, Instant.now());
        racingGame.updateSpeed(
                players.get(1).getName().value(), 10, (lastTapedTime, now, tapCount) -> 10, Instant.now());

        // when
        racingGame.moveAll(Instant.now());

        // then
        final Map<Runner, Integer> positions = racingGame.getPositions();
        assertThat(positions.values()).allMatch(position -> position == 10);
    }

    @Test
    void 모든_러너가_결승선에_도착한다() {
        // given
        racingGame.setUp(players.stream().map(p -> p.toGamer()).toList());
        racingGame.updateState(RacingGameState.PLAYING);

        racingGame.updateSpeed(
                players.getFirst().getName().value(), 10, (lastTapedTime, now, tapCount) -> 30, Instant.now());
        racingGame.updateSpeed(
                players.get(1).getName().value(), 10, (lastTapedTime, now, tapCount) -> 30, Instant.now());

        // when
        for (int i = 0; i < 101; ++i) {
            racingGame.moveAll(Instant.now());
        }

        // then
        assertThat(racingGame.isFinished()).isTrue();
    }

    @Test
    void 러너의_속도를_조절한다() {
        // given
        racingGame.setUp(players.stream().map(p -> p.toGamer()).toList());
        racingGame.updateState(RacingGameState.PLAYING);

        racingGame.updateSpeed(
                players.getFirst().getName().value(), 10, (lastTapedTime, now, tapCount) -> 10, Instant.now());
        racingGame.updateSpeed(
                players.get(1).getName().value(), 10, (lastTapedTime, now, tapCount) -> 10, Instant.now());

        // then
        assertThat(racingGame.getRunners().getSpeeds().values()).allMatch(value -> value == 10);
    }

    @Test
    void 게임이_진행_중이_아니면_속도_조정시_예외가_발생한다() {
        // given
        final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> 10;
        racingGame.setUp(players.stream().map(p -> p.toGamer()).toList());

        // when && then
        assertThatThrownBy(() -> racingGame.updateSpeed(
                        players.getFirst().getName().value(), 10, speedCalculator, Instant.now()))
                .isInstanceOf(BusinessException.class);
    }

    // 완주 순서는 tick 시각으로만 갈린다. 점수는 밀리초로 절삭되므로(RacingGame#convertScore)
    // 실제 시각으로 돌리면 300틱이 같은 밀리초에 들어가 동률이 되고 순위가 뒤집힌다.
    // startTime을 기준으로 MOVE_INTERVAL_MILLIS 간격의 가짜 tick을 주입해 결정적으로 만든다.
    @Test
    void 게임_결과를_조회할_수_있다() {
        // given
        racingGame.setUp(players.stream().map(p -> p.toGamer()).toList());
        racingGame.updateState(RacingGameState.PLAYING);
        racingGame.setUpStart();
        racingGame.setAutoMoveFuture(null);

        final Instant startTime = racingGame.getStartTime();
        int tick = 0;

        for (int i = 0; i < 100; i++) {
            final Instant now = startTime.plusMillis(++tick * RacingGame.MOVE_INTERVAL_MILLIS);
            racingGame.updateSpeed(players.get(1).getName().value(), 10, (lastTapedTime, at, tapCount) -> 30, now);
            racingGame.updateSpeed(players.getFirst().getName().value(), 10, (lastTapedTime, at, tapCount) -> 10, now);
            racingGame.moveAll(now);
        }

        for (int i = 0; i < 200; i++) {
            final Instant now = startTime.plusMillis(++tick * RacingGame.MOVE_INTERVAL_MILLIS);
            racingGame.updateSpeed(players.getFirst().getName().value(), 10, (lastTapedTime, at, tapCount) -> 10, now);
            racingGame.moveAll(now);
        }

        // when
        final var result = racingGame.getResult();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRank().get(players.getFirst().toGamer())).isEqualTo(2);
        assertThat(result.getRank().get(players.get(1).toGamer())).isEqualTo(1);
    }

    /**
     * 회귀 가드: {@code stopAutoMove()}는 자동 이동 태스크 자신의 스레드에서 호출된다
     * (RacingGameService.handleRaceFinished ← executeAutoMove). {@code cancel(true)}로 취소하면
     * 실행 중인 그 스레드가 스스로를 인터럽트하고, 곧이어 같은 스레드에서 발행되는
     * MiniGameFinishedEvent의 결과 저장 리스너가 @RedisLock의 tryLock에서
     * InterruptedException으로 실패한다 — 레이싱 기록이 한 건도 저장되지 않는다.
     */
    @Test
    void 자동_이동_태스크_안에서_정지시켜도_실행_스레드가_인터럽트되지_않는다() throws InterruptedException {
        // given
        racingGame.setUp(players.stream().map(p -> p.toGamer()).toList());
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        final CountDownLatch futureAssigned = new CountDownLatch(1);
        final CountDownLatch stopped = new CountDownLatch(1);
        final AtomicBoolean interrupted = new AtomicBoolean();

        // when — 태스크가 자기 자신을 취소한 뒤 같은 스레드에서 이어지는 구간을 재현한다
        final ScheduledFuture<?> autoMoveFuture = scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        futureAssigned.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    racingGame.stopAutoMove();
                    interrupted.set(Thread.currentThread().isInterrupted());
                    stopped.countDown();
                },
                0,
                100,
                TimeUnit.MILLISECONDS);
        racingGame.setAutoMoveFuture(autoMoveFuture);
        futureAssigned.countDown();

        // then — 취소는 하되 인터럽트는 하지 않는다. 인터럽트만 검증하면 stopAutoMove()가
        // 통째로 no-op이 돼도(자동 이동이 영원히 도는 더 큰 회귀) 통과한다.
        try {
            assertThat(stopped.await(3, TimeUnit.SECONDS)).as("자동 이동 태스크 실행 완료").isTrue();
            assertSoftly(softly -> {
                softly.assertThat(interrupted).as("실행 스레드 인터럽트 여부").isFalse();
                softly.assertThat(autoMoveFuture.isCancelled())
                        .as("자동 이동 취소 여부")
                        .isTrue();
            });
        } finally {
            scheduler.shutdownNow();
        }
    }
}
