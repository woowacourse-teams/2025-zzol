package coffeeshout.nunchi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.nunchi.config.NunchiTimingProperties;
import coffeeshout.nunchi.domain.NunchiGame;
import coffeeshout.nunchi.domain.NunchiState;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * {@link NunchiFlowOrchestrator} 단위 테스트. 스케줄러를 캡처 mock으로 갈아끼워 타이머
 * cancel/reschedule와 종료 멱등성을 결정적으로 검증한다(ADR-0031 — Flow가 난이도 최상).
 *
 * <p>캡처 mock {@link CapturingScheduler}는 {@code schedule(task, instant)}마다 {@code (task, future)}를
 * 기록하고, 테스트가 임의 시점에 task를 수동 발화하거나 future가 cancel됐는지 확인하게 한다(실제 시간 대기 없음).
 */
class NunchiFlowOrchestratorTest {

    private static final JoinCode JOIN_CODE = new JoinCode("ABCD");
    private static final Instant T0 = Instant.parse("2026-06-21T00:00:00Z");

    private final Gamer 일 = Gamer.of("일", null);
    private final Gamer 이 = Gamer.of("이", null);
    private final Gamer 삼 = Gamer.of("삼", null);

    private CapturingScheduler scheduler;
    private NunchiNotifier notifier;
    private GameSessionService gameSessionService;
    private ApplicationEventPublisher eventPublisher;
    private NunchiFlowOrchestrator orchestrator;
    private NunchiGame game;

    @BeforeEach
    void setUp() {
        scheduler = new CapturingScheduler();
        notifier = mock(NunchiNotifier.class);
        gameSessionService = mock(GameSessionService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        when(gameSessionService.finishGame(any())).thenReturn(1);

        final NunchiTimingProperties timing = new NunchiTimingProperties(
                Duration.ofMillis(300),   // numberWindow
                Duration.ofMillis(2000),  // collisionCooldown
                Duration.ofMillis(10000), // idleTimeout
                Duration.ofMillis(30000), // hardCap
                Duration.ofMillis(2000)   // earlyFinishDelay
        );
        orchestrator = new NunchiFlowOrchestrator(
                scheduler, timing, notifier, gameSessionService, eventPublisher);

        game = new NunchiGame(300L);
        game.setUp(List.of(일, 이, 삼));
    }

    @Nested
    class 시작 {

        @Test
        void startFlow는_PLAYING을_브로드캐스트하고_idle과_하드캡_타이머를_건다() {
            orchestrator.startFlow(game, JOIN_CODE);

            verify(notifier).notifyPlaying(eq(JOIN_CODE.getValue()), eq(1), any(),
                    anyLong(), anyLong(), anyLong());
            // idle + hardCap 두 타이머가 예약된다
            assertThat(scheduler.scheduledCount()).isEqualTo(2);
        }
    }

    @Nested
    class STOOD_분기 {

        @Test
        void STOOD이면_stand를_브로드캐스트하고_윈도우_타이머를_새로_건다() {
            orchestrator.startFlow(game, JOIN_CODE);
            final int before = scheduler.scheduledCount();

            orchestrator.handlePress(game, JOIN_CODE, 일, T0);

            verify(notifier).notifyStood(eq(JOIN_CODE.getValue()), eq("일"), eq(1), anyLong(), anyLong());
            // 윈도우 + idle 리셋 = 최소 2개 추가 예약
            assertThat(scheduler.scheduledCount()).isGreaterThan(before);
        }

        @DisplayName("STOOD→STOOD(윈도우 경과): 이전 윈도우 future가 cancel되고 새 윈도우가 잡힌다")
        @Test
        void 연속_STOOD는_윈도우를_cancel하고_reschedule한다() {
            orchestrator.startFlow(game, JOIN_CODE);

            orchestrator.handlePress(game, JOIN_CODE, 일, T0);
            final ScheduledFuture<?> firstWindow = scheduler.lastFuture();

            // 윈도우(300ms) 밖의 두 번째 press → 직전 pending solo 확정 + 새 STOOD
            orchestrator.handlePress(game, JOIN_CODE, 이, T0.plusMillis(1000));

            // 첫 윈도우 future는 reschedule되며 cancel돼야 한다
            verify(firstWindow).cancel(false);
            assertThat(game.getCurrentNumber()).isEqualTo(2); // 일이 1번 solo 확정
        }
    }

    @Nested
    class COLLIDED_분기 {

        @DisplayName("STOOD→COLLIDED(윈도우 내): 윈도우 cancel + COLLISION_COOLDOWN 브로드캐스트 + 쿨다운 예약")
        @Test
        void 윈도우_내_동시_press는_충돌로_처리된다() {
            orchestrator.startFlow(game, JOIN_CODE);

            orchestrator.handlePress(game, JOIN_CODE, 일, T0);
            final ScheduledFuture<?> windowFuture = scheduler.lastFuture();

            orchestrator.handlePress(game, JOIN_CODE, 이, T0.plusMillis(100)); // 윈도우 내 → 충돌

            verify(windowFuture).cancel(false); // 충돌로 윈도우 종료
            verify(notifier).notifyCollisionCooldown(eq(JOIN_CODE.getValue()), eq(1),
                    eq(List.of("일", "이")), anyLong(), anyLong());
            assertThat(game.getState()).isEqualTo(NunchiState.COLLISION_COOLDOWN);
        }
    }

    @Nested
    class 종료_멱등성 {

        @Test
        void 전원_입력_완료시_즉시_DONE을_한_번만_발행한다() {
            orchestrator.startFlow(game, JOIN_CODE);

            // 일·이 충돌(2명 OUT) 후 삼 solo → 전원 입력 완료
            orchestrator.handlePress(game, JOIN_CODE, 일, T0);
            orchestrator.handlePress(game, JOIN_CODE, 이, T0.plusMillis(100)); // 충돌
            fireCooldown();
            orchestrator.handlePress(game, JOIN_CODE, 삼, T0.plusMillis(5000)); // solo → 전원 입력

            verify(notifier, times(1)).notifyDone(JOIN_CODE.getValue());
            assertThat(game.isFinished()).isTrue();

            // MiniGameFinishedEvent는 earlyFinishDelay만큼 지연 발행된다 — 캡처된 마지막 task를 발화해 확인
            scheduler.lastTask().run();
            verify(eventPublisher, times(1)).publishEvent(any(MiniGameFinishedEvent.class));
        }

        @DisplayName("종료 후 늦게 발화한 idle/하드캡 콜백은 다시 종료하지 않는다(멱등)")
        @Test
        void 종료_후_타이머_콜백은_무시된다() {
            orchestrator.startFlow(game, JOIN_CODE);
            // startFlow가 건 idle·hardCap 콜백 핸들을 캡처
            final List<Runnable> startupTimers = scheduler.allTasksSnapshot();

            // 전원 충돌로 즉시 종료
            orchestrator.handlePress(game, JOIN_CODE, 일, T0);
            orchestrator.handlePress(game, JOIN_CODE, 이, T0.plusMillis(100));
            fireCooldown();
            orchestrator.handlePress(game, JOIN_CODE, 삼, T0.plusMillis(5000));

            verify(notifier, times(1)).notifyDone(JOIN_CODE.getValue());

            // 종료 후 startFlow의 idle/hardCap 콜백(stale)을 강제로 발화해도 추가 종료 없음
            startupTimers.forEach(Runnable::run);

            // notifyDone은 여전히 1회 — stale 타이머가 멱등 가드(finished)에 막힌다
            verify(notifier, times(1)).notifyDone(JOIN_CODE.getValue());
        }

        @Test
        void idle_타임아웃이_먼저_발화하면_미입력자를_MISS로_종료한다() {
            orchestrator.startFlow(game, JOIN_CODE);

            // idle 타이머(startFlow의 첫 예약 — 인덱스 0=idle, 1=hardCap)를 강제 발화
            scheduler.taskAt(0).run();

            verify(notifier, times(1)).notifyDone(JOIN_CODE.getValue());
            assertThat(game.isFinished()).isTrue();
        }

        @DisplayName("press로 idle을 리셋한 뒤 발화한 stale idle 타이머는 게임을 종료하지 않는다(★ 동시성)")
        @Test
        void 대체된_stale_idle_타이머는_종료를_트리거하지_않는다() {
            orchestrator.startFlow(game, JOIN_CODE);
            final Runnable staleIdle = scheduler.taskAt(0); // startFlow가 건 idle

            // 유효 press가 idle을 리셋(scheduleIdle이 새 idle을 걸고 generation을 올림)
            orchestrator.handlePress(game, JOIN_CODE, 일, T0);

            // cancel(false)는 이미 발화해 락 대기 중인 옛 콜백을 못 막는다 — 강제 발화해 stale 가드를 검증
            staleIdle.run();

            // generation 불일치로 무시 — 종료되지 않아야 한다
            verify(notifier, never()).notifyDone(anyString());
            assertThat(game.isFinished()).isFalse();
        }
    }

    @Nested
    class 고정_데드라인 {

        @DisplayName("hardCapEpochMs는 시작 시 고정되어 이후 PLAYING 브로드캐스트에서 변하지 않는다(결정 8)")
        @Test
        void 하드캡은_재개_브로드캐스트에서도_고정값이다() {
            orchestrator.startFlow(game, JOIN_CODE);
            final ArgumentCaptor<Long> hardCapCaptor = ArgumentCaptor.forClass(Long.class);
            verify(notifier).notifyPlaying(eq(JOIN_CODE.getValue()), anyInt(), any(),
                    anyLong(), anyLong(), hardCapCaptor.capture());
            final long startHardCap = hardCapCaptor.getValue();

            // 충돌 → 쿨다운 종료(재개)에서 PLAYING이 다시 브로드캐스트된다
            orchestrator.handlePress(game, JOIN_CODE, 일, T0);
            orchestrator.handlePress(game, JOIN_CODE, 이, T0.plusMillis(100)); // 충돌
            fireCooldown(); // onCooldownEnd → broadcastPlaying

            final ArgumentCaptor<Long> resumeHardCap = ArgumentCaptor.forClass(Long.class);
            verify(notifier, times(2)).notifyPlaying(eq(JOIN_CODE.getValue()), anyInt(), any(),
                    anyLong(), anyLong(), resumeHardCap.capture());
            // 마지막(재개) 브로드캐스트의 hardCap이 시작값과 동일해야 한다 — 드리프트 금지
            assertThat(resumeHardCap.getValue()).isEqualTo(startHardCap);
        }
    }

    /** COLLISION_COOLDOWN 직후 예약된 쿨다운 콜백을 찾아 발화한다(마지막으로 예약된 task가 쿨다운). */
    private void fireCooldown() {
        scheduler.lastTask().run();
    }

    /**
     * 스케줄된 {@code (task, future)}를 순서대로 기록하는 캡처 스케줄러. {@code TestTaskScheduler}처럼
     * 실제 {@code ThreadPoolTaskScheduler}를 상속하되 {@code schedule(task, Instant)}만 가로채 즉시 실행하지
     * 않고 캡처한다. future는 mock이라 cancel 호출을 검증할 수 있고, task는 수동 발화로 타이머 만료를 흉내낸다.
     */
    private static final class CapturingScheduler
            extends org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler {

        private final transient List<Runnable> tasks = new ArrayList<>();
        private final transient List<ScheduledFuture<?>> futures = new ArrayList<>();

        CapturingScheduler() {
            this.setPoolSize(1);
            this.initialize();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            final ScheduledFuture<?> future = mock(ScheduledFuture.class);
            tasks.add(task);
            futures.add(future);
            return future;
        }

        int scheduledCount() {
            return tasks.size();
        }

        ScheduledFuture<?> lastFuture() {
            return futures.get(futures.size() - 1);
        }

        Runnable lastTask() {
            return tasks.get(tasks.size() - 1);
        }

        Runnable taskAt(int index) {
            return tasks.get(index);
        }

        List<Runnable> allTasksSnapshot() {
            return new ArrayList<>(tasks);
        }
    }
}
