package coffeeshout.wormgame.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import coffeeshout.fixture.GamerFixture;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.wormgame.config.WormGameRulesProperties;
import coffeeshout.wormgame.config.WormGameTimingProperties;
import coffeeshout.wormgame.domain.WormGame;
import coffeeshout.wormgame.domain.WormGameState;
import coffeeshout.wormgame.domain.event.WormGameStateChangedEvent;
import coffeeshout.wormgame.domain.event.WormsMovedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 흐름 전이·종료 순서·틱 루프 취소를 가짜 스케줄러로 검증한다(NunchiFlowOrchestratorTest의 CapturingScheduler 패턴).
 * 아레나 반지름을 1u로 줘 첫 틱에 전원이 경계 밖으로 나가 라운드가 끝나게 만든다.
 */
@ExtendWith(MockitoExtension.class)
class WormGameFlowTest {

    private static final String JOIN_CODE = "ABCD";

    @Mock
    private GameSessionService gameSessionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final CapturingScheduler scheduler = new CapturingScheduler();
    private WormGameService service;
    private WormGame game;

    @BeforeEach
    void setUp() {
        final WormGameRulesProperties rules = new WormGameRulesProperties(
                50L, 120.0, 1.6, 1200, 200.0, 0.7, 1.0, 200, 1200, 0.30, 0.05, 0.5, 0, 6.0, 3, 5);
        final WormGameTimingProperties timing = new WormGameTimingProperties(
                Duration.ofMillis(1), Duration.ofMillis(1), Duration.ofMillis(1), Duration.ofSeconds(10));
        service = new WormGameService(gameSessionService, scheduler, eventPublisher, timing, rules);

        game = new WormGame(rules.toRules());
        game.setUp(GamerFixture.꾹이_루키_엠제이_한스());
        final GameSession session = mock(GameSession.class);
        given(gameSessionService.getSession(any())).willReturn(session);
        given(session.findCompletedGame(MiniGameType.WORM_GAME)).willReturn(game);
    }

    private void startAndReachPlaying() {
        service.start(JOIN_CODE, "꾹이");
        scheduler.taskAt(0).run(); // description 만료 → PREPARE
        scheduler.taskAt(1).run(); // prepare 만료 → PLAYING + 틱 루프 등록
    }

    @Test
    void 시작하면_DESCRIPTION_PREPARE_PLAYING_순서로_전이되고_틱_루프를_등록한다() {
        // when
        startAndReachPlaying();

        // then
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(game.getState()).isEqualTo(WormGameState.PLAYING);
        softly.assertThat(scheduler.tickTask()).isNotNull();
        softly.assertAll();
        final InOrder inOrder = inOrder(eventPublisher);
        inOrder.verify(eventPublisher).publishEvent(stateChangedTo(WormGameState.DESCRIPTION));
        inOrder.verify(eventPublisher).publishEvent(stateChangedTo(WormGameState.PREPARE));
        inOrder.verify(eventPublisher).publishEvent(stateChangedTo(WormGameState.PLAYING));
    }

    @Test
    void 라운드가_끝나면_틱을_취소하고_FINISH_뒤_DONE에서_세션_확정_상태_알림_결과_이벤트_순으로_발행한다() {
        // given
        startAndReachPlaying();
        given(gameSessionService.finishGame(any())).willReturn(1);

        // when — 반지름 1u라 첫 틱에 전원 경계 밖 → 라운드 종료
        scheduler.tickTask().run();
        assertThat(game.getState()).isEqualTo(WormGameState.FINISH);
        then(scheduler.tickFuture()).should().cancel(false);
        scheduler.lastTask().run(); // finish 연출 만료 → DONE

        // then — ADR-0025 순서 불변식
        assertThat(game.getState()).isEqualTo(WormGameState.DONE);
        final InOrder inOrder = inOrder(gameSessionService, eventPublisher);
        inOrder.verify(gameSessionService).finishGame(any());
        inOrder.verify(eventPublisher).publishEvent(stateChangedTo(WormGameState.DONE));
        inOrder.verify(eventPublisher).publishEvent(any(MiniGameFinishedEvent.class));
    }

    @Test
    void 틱_처리_중_예외가_나면_루프를_중단하고_FINISH_뒤_DONE까지_도달한다() {
        // given
        startAndReachPlaying();
        given(gameSessionService.finishGame(any())).willReturn(1);
        willThrow(new IllegalStateException("broker down"))
                .given(eventPublisher)
                .publishEvent(any(WormsMovedEvent.class));

        // when
        scheduler.tickTask().run();

        // then — 루프를 끊는 것만으로는 부족하다. 상태가 PLAYING 에 남으면 세션이 안 풀려 룰렛까지 막힌다
        then(scheduler.tickFuture()).should().cancel(false);
        assertThat(game.getState()).isEqualTo(WormGameState.FINISH);

        // when — 종료 연출 만료
        scheduler.lastTask().run();

        // then
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(game.getState()).isEqualTo(WormGameState.DONE);
        softly.assertAll();
        then(gameSessionService).should().finishGame(any());
    }

    @Test
    void PLAYING이_아니면_늦게_도착한_틱은_무시한다() {
        // given
        startAndReachPlaying();
        scheduler.tickTask().run(); // → FINISH (done은 아직 안 돔)
        final long ticksAtFinish = game.getTickCount();

        // when — 취소 직전에 이미 큐에 있던 틱이 한 번 더 돈다
        scheduler.tickTask().run();

        // then
        assertThat(game.getTickCount()).isEqualTo(ticksAtFinish);
    }

    private static Object stateChangedTo(WormGameState state) {
        return argThat(event -> event instanceof WormGameStateChangedEvent changed && changed.state() == state);
    }

    /** 지연 태스크와 고정 주기 틱을 캡처하는 스케줄러 — future는 mock이라 cancel을 검증할 수 있다. */
    private static final class CapturingScheduler extends ThreadPoolTaskScheduler {

        private final transient List<Runnable> tasks = new ArrayList<>();
        private transient Runnable tickTask;
        private transient ScheduledFuture<?> tickFuture;

        CapturingScheduler() {
            this.setPoolSize(1);
            this.initialize();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            tasks.add(task);
            return mock(ScheduledFuture.class);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            tickTask = task;
            tickFuture = mock(ScheduledFuture.class);
            return tickFuture;
        }

        Runnable taskAt(int index) {
            return tasks.get(index);
        }

        Runnable lastTask() {
            return tasks.getLast();
        }

        Runnable tickTask() {
            return tickTask;
        }

        ScheduledFuture<?> tickFuture() {
            return tickFuture;
        }
    }
}
