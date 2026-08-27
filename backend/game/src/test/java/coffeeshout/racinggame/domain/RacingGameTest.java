package coffeeshout.racinggame.domain;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.minigame.domain.MiniGameResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RacingGameTest {

    final RacingGame racingGame = new RacingGame();
    final Player 한스 = PlayerFixture.호스트한스();
    final Player 꾹이 = PlayerFixture.게스트꾹이();

    @BeforeEach
    void setUp() {
        racingGame.setUp(List.of(한스.toGamer(), 꾹이.toGamer()));
    }

    @Nested
    class 게임_준비 {

        @Test
        void 준비를_마치면_설명_상태로_모든_러너가_출발선에_선다() {
            assertSoftly(softly -> {
                softly.assertThat(racingGame.getState()).isEqualTo(RacingGameState.DESCRIPTION);
                softly.assertThat(racingGame.getPositions()).hasSize(2);
                softly.assertThat(racingGame.getPositions().values())
                        .allMatch(position -> position == RacingGame.START_LINE);
            });
        }
    }

    @Nested
    class 러너_이동 {

        @Test
        void 한_틱을_지나면_속도만큼_전진한다() {
            // given
            경주를_시작한다();

            // when
            달린다(1, 1, Map.of(한스, 10, 꾹이, 10));

            // then
            assertThat(racingGame.getPositions().values()).allMatch(position -> position == 10);
        }

        @Test
        void 계속_탭하면_모든_러너가_결승선에_도착한다() {
            // given
            경주를_시작한다();

            // when — 결승선 3000 을 속도 30 으로 넘기려면 100 틱이 필요하다
            달린다(1, 101, Map.of(한스, 30, 꾹이, 30));

            // then
            assertThat(racingGame.isFinished()).isTrue();
        }
    }

    @Nested
    class 속도_조절 {

        @Test
        void 탭하면_계산된_속도가_반영된다() {
            // given
            경주를_시작한다();

            // when
            탭한다(Map.of(한스, 10, 꾹이, 10), 틱_시각(1));

            // then
            assertThat(racingGame.getRunners().getSpeeds().values()).allMatch(speed -> speed == 10);
        }

        @Test
        void 게임이_진행_중이_아니면_속도_조정시_예외가_발생한다() {
            // given — setUp 직후는 DESCRIPTION 상태다
            final SpeedCalculator speedCalculator = (lastTapedTime, now, tapCount) -> 10;

            // when & then
            assertCoffeeShoutException(
                    () -> racingGame.updateSpeed(한스.getName().value(), 10, speedCalculator, Instant.now()),
                    RacingGameErrorCode.NOT_PLAYING_STATE);
        }
    }

    @Nested
    class 게임_결과 {

        @Test
        void 먼저_완주한_러너가_1등이다() {
            // given
            경주를_시작한다();

            // when — 꾹이는 속도 30 으로 100 틱에, 한스는 속도 10 으로 300 틱에 완주한다
            달린다(1, 100, Map.of(꾹이, 30, 한스, 10));
            달린다(101, 300, Map.of(한스, 10));

            // then
            final MiniGameResult result = racingGame.getResult();
            assertSoftly(softly -> {
                softly.assertThat(result.getRank().get(꾹이.toGamer())).isEqualTo(1);
                softly.assertThat(result.getRank().get(한스.toGamer())).isEqualTo(2);
            });
        }
    }

    @Nested
    class 주행_중_감속 {

        @Test
        void 최고_속도를_찍고_탭을_멈추면_결승선까지_자동_주행하지_않는다() {
            // given — 한 틱 만에 최고 속도에 올린다
            경주를_시작한다();
            달린다(1, 1, Map.of(한스, RacingGame.MAX_SPEED));

            // when — 속도가 굳어 있다면 3000 / 60 = 50 틱이면 완주한다
            달린다(2, 50, Map.of());

            // then
            assertThat(러너(한스).getPosition()).isLessThan(RacingGame.FINISH_LINE);
        }

        @Test
        void 탭을_멈추면_속도가_줄어든다() {
            // given
            경주를_시작한다();
            달린다(1, 1, Map.of(한스, RacingGame.MAX_SPEED));

            // when
            달린다(2, 6, Map.of());

            // then
            assertThat(러너(한스).getSpeed()).isLessThan(RacingGame.MAX_SPEED);
        }

        @Test
        void 속도는_최저_속도_아래로는_떨어지지_않는다() {
            // given — 최저 속도 아래로 떨어지면 isStopped()가 되어 완주하지 못한 채 경주가 끝난다
            경주를_시작한다();
            달린다(1, 1, Map.of(한스, RacingGame.MAX_SPEED));

            // when
            달린다(2, 200, Map.of());

            // then
            assertSoftly(softly -> {
                softly.assertThat(러너(한스).getSpeed()).isEqualTo(RacingGame.MIN_SPEED);
                softly.assertThat(러너(한스).isStopped()).isFalse();
            });
        }

        @Test
        void 계속_탭한_러너가_몰아치고_멈춘_러너보다_앞선다() {
            // given — 둘 다 최고 속도에서 출발한다
            경주를_시작한다();
            달린다(1, 1, Map.of(한스, RacingGame.MAX_SPEED, 꾹이, RacingGame.MAX_SPEED));

            // when — 한스만 계속 탭한다
            달린다(2, 40, Map.of(한스, RacingGame.MAX_SPEED));

            // then
            assertThat(러너(한스).getPosition()).isGreaterThan(러너(꾹이).getPosition());
        }

        @Test
        void 완주한_러너는_주행_중_감속이_아니라_완주_후_감속으로_멈춘다() {
            // given — 완주 직후의 속도를 확인한다
            경주를_시작한다();
            달린다(1, 50, Map.of(한스, RacingGame.MAX_SPEED));
            final int 완주_직후_속도 = 러너(한스).getSpeed();

            // when — 완주 후에는 탭이 무시되므로 한 틱만 더 돌린다
            달린다(51, 51, Map.of(한스, RacingGame.MAX_SPEED));

            // then — 완주 후 감속은 비율이 아니라 SLOW_DOWN_STEP 만큼 줄어든다
            assertSoftly(softly -> {
                softly.assertThat(러너(한스).isFinished()).isTrue();
                softly.assertThat(러너(한스).getSpeed()).isEqualTo(완주_직후_속도 - Runner.SLOW_DOWN_STEP);
            });
        }
    }

    @Nested
    class 자동_이동_정지 {

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
                assertThat(stopped.await(3, TimeUnit.SECONDS))
                        .as("자동 이동 태스크 실행 완료")
                        .isTrue();
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

    private Runner 러너(Player player) {
        return racingGame.getRunners().stream()
                .filter(runner ->
                        runner.getGamer().getName().equals(player.getName().value()))
                .findFirst()
                .orElseThrow();
    }

    private void 경주를_시작한다() {
        racingGame.updateState(RacingGameState.PLAYING);
        racingGame.setUpStart();
    }

    /**
     * 자동 이동 스케줄러를 대신해 tick 구간을 돌린다. {@code speeds}에 담긴 러너는 매 틱 탭한 것으로 보고,
     * 빠진 러너는 탭을 멈춘 것으로 본다.
     */
    private void 달린다(int 시작틱, int 끝틱, Map<Player, Integer> speeds) {
        for (int tick = 시작틱; tick <= 끝틱; tick++) {
            final Instant now = 틱_시각(tick);
            탭한다(speeds, now);
            racingGame.moveAll(now);
        }
    }

    private void 탭한다(Map<Player, Integer> speeds, Instant now) {
        speeds.forEach((player, speed) ->
                racingGame.updateSpeed(player.getName().value(), 10, (lastTapedTime, at, tapCount) -> speed, now));
    }

    /**
     * 실제 시계 대신 startTime 기준 {@code MOVE_INTERVAL_MILLIS} 간격의 고정 시각을 쓴다.
     * 완주 시각은 밀리초로 절삭되므로(RacingGame#convertScore), 실제 시계로 돌리면 수백 틱이
     * 같은 밀리초에 몰려 순위가 비결정적이 된다.
     */
    private Instant 틱_시각(int tick) {
        return racingGame.getStartTime().plusMillis(tick * RacingGame.MOVE_INTERVAL_MILLIS);
    }
}
