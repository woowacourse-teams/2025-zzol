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
        racingGame.moveAll();

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
            racingGame.moveAll();
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

    // PMD.NoThreadSleep 억제 — 아래 Thread.sleep은 테스트 편의가 아니라 프로덕션 제약이다.
    // RacingGame.moveAll()이 내부에서 Instant.now()를 읽고(RacingGame:50) getResult()가 그
    // finishTime으로 순위를 매기므로, 두 주자의 완주 시각을 벌리지 않으면 순위가 비결정적이 된다.
    // 근본 해결은 moveAll()이 Instant를 주입받는 것(컨벤션: 시간은 파라미터로 주입)이며,
    // 프로덕션 API 변경이라 별도 이슈로 분리한다.
    @SuppressWarnings("PMD.NoThreadSleep")
    @Test
    void 게임_결과를_조회할_수_있다() throws InterruptedException {
        // given
        racingGame.setUp(players.stream().map(p -> p.toGamer()).toList());
        racingGame.updateState(RacingGameState.PLAYING);
        racingGame.setUpStart();
        racingGame.setAutoMoveFuture(null);

        for (int i = 0; i < 100; i++) {
            racingGame.updateSpeed(
                    players.get(1).getName().value(), 10, (lastTapedTime, now, tapCount) -> 30, Instant.now());
            racingGame.updateSpeed(
                    players.getFirst().getName().value(), 10, (lastTapedTime, now, tapCount) -> 10, Instant.now());
            racingGame.moveAll();
        }

        Thread.sleep(2);

        for (int i = 0; i < 200; i++) {
            racingGame.updateSpeed(
                    players.getFirst().getName().value(), 10, (lastTapedTime, now, tapCount) -> 10, Instant.now());
            racingGame.moveAll();
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
