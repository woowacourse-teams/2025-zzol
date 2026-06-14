package coffeeshout.blindtimer.application;

import coffeeshout.blindtimer.config.BlindTimerGameTimingProperties;
import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blindtimer.domain.BlindTimerGameState;
import coffeeshout.blindtimer.domain.event.BlindTimerFinishedEvent;
import coffeeshout.blindtimer.domain.event.BlindTimerProgressEvent;
import coffeeshout.blindtimer.domain.event.BlindTimerStateChangedEvent;
import coffeeshout.game.metric.GameDurationMetricService;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BlindTimerGameService implements MiniGameService {

    private final GameSessionService gameSessionService;
    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;
    private final BlindTimerGameTimingProperties timing;
    private final GameDurationMetricService gameDurationMetricService;

    public BlindTimerGameService(
            GameSessionService gameSessionService,
            @Qualifier("blindTimerGameScheduler") TaskScheduler taskScheduler,
            ApplicationEventPublisher eventPublisher,
            BlindTimerGameTimingProperties timing,
            GameDurationMetricService gameDurationMetricService
    ) {
        this.gameSessionService = gameSessionService;
        this.taskScheduler = taskScheduler;
        this.eventPublisher = eventPublisher;
        this.timing = timing;
        this.gameDurationMetricService = gameDurationMetricService;
    }

    @Override
    public void start(String joinCode, String hostName) {
        final BlindTimerGame game = getBlindTimerGame(new JoinCode(joinCode));

        scheduleDescription(game, joinCode);

        eventPublisher.publishEvent(stateEvent(game, joinCode));
        log.info("블라인드 타이머 게임 시작: joinCode={}, targetTime={}", joinCode, game.getTargetTime());
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.BLIND_TIMER;
    }

    public void finishGame(BlindTimerGame game, String joinCode) {
        if (!game.tryFinish()) {
            return;
        }
        game.cancelTimeout();

        // 순서 불변식(ADR-0025 결정 5): finishGame()으로 roundCount를 먼저 확정·상태 복귀시킨다.
        final int roundCount = gameSessionService.finishGame(new JoinCode(joinCode));

        eventPublisher.publishEvent(BlindTimerProgressEvent.of(game, joinCode));
        taskScheduler.schedule(
                () -> eventPublisher.publishEvent(BlindTimerFinishedEvent.of(game, joinCode)),
                Instant.now().plus(timing.resultDelay())
        );
        // 확률 조정·결과 저장을 유발하는 이벤트는 종료 알림을 모두 보낸 뒤 마지막에 발행한다 —
        // 저장 리스너(@Transactional/@RedisLock) 실패가 게임 종료 알림을 막지 않도록(다른 게임과 동일 순서).
        eventPublisher.publishEvent(new MiniGameFinishedEvent(
                joinCode, MiniGameType.BLIND_TIMER.name(), game.getResult().toRankMap(), roundCount));
        log.info("블라인드 타이머 게임 종료: joinCode={}", joinCode);
    }

    public BlindTimerGame getBlindTimerGame(JoinCode joinCode) {
        return (BlindTimerGame) gameSessionService.getSession(joinCode)
                .findCompletedGame(MiniGameType.BLIND_TIMER);
    }

    private void scheduleDescription(BlindTimerGame game, String joinCode) {
        game.updateState(BlindTimerGameState.DESCRIPTION);
        taskScheduler.schedule(
                () -> schedulePrepare(game, joinCode),
                Instant.now().plus(timing.description())
        );
    }

    private void schedulePrepare(BlindTimerGame game, String joinCode) {
        game.updateState(BlindTimerGameState.PREPARE);
        eventPublisher.publishEvent(stateEvent(game, joinCode));
        eventPublisher.publishEvent(BlindTimerProgressEvent.of(game, joinCode));

        taskScheduler.schedule(
                () -> startPlaying(game, joinCode),
                Instant.now().plus(timing.prepare())
        );
    }

    private void startPlaying(BlindTimerGame game, String joinCode) {
        game.startPlaying();
        eventPublisher.publishEvent(stateEvent(game, joinCode));
        gameDurationMetricService.startGameTimer(joinCode);

        final Duration timeout = game.getTargetTime().plus(timing.timeoutBuffer());
        final ScheduledFuture<?> timeoutFuture = taskScheduler.schedule(
                () -> handleTimeout(game, joinCode),
                game.getStartTime().plus(timeout)
        );
        game.setTimeoutFuture(timeoutFuture);
    }

    private void handleTimeout(BlindTimerGame game, String joinCode) {
        log.info("블라인드 타이머 게임 타임아웃: joinCode={}", joinCode);
        game.markAllTimedOut();
        finishGame(game, joinCode);
    }

    private BlindTimerStateChangedEvent stateEvent(BlindTimerGame game, String joinCode) {
        return BlindTimerStateChangedEvent.of(game, joinCode, timing.blindDelay());
    }
}
