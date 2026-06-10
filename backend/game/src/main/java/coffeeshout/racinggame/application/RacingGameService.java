package coffeeshout.racinggame.application;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.racinggame.config.RacingGameTimingProperties;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.RacingGameState;
import coffeeshout.racinggame.domain.SpeedCalculator;
import coffeeshout.racinggame.domain.event.RaceFinishedEvent;
import coffeeshout.racinggame.domain.event.RaceStateChangedEvent;
import coffeeshout.racinggame.domain.event.RunnersMovedEvent;
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
public class RacingGameService implements MiniGameService {

    private final GameSessionService gameSessionService;
    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;
    private final SpeedCalculator speedCalculator;
    private final RacingGameTimingProperties timing;

    public RacingGameService(
            GameSessionService gameSessionService,
            @Qualifier("racingGameScheduler") TaskScheduler taskScheduler,
            ApplicationEventPublisher eventPublisher,
            SpeedCalculator speedCalculator,
            RacingGameTimingProperties timing
    ) {
        this.gameSessionService = gameSessionService;
        this.taskScheduler = taskScheduler;
        this.eventPublisher = eventPublisher;
        this.speedCalculator = speedCalculator;
        this.timing = timing;
    }

    @Override
    public void start(String joinCode, String hostName) {
        final RacingGame racingGame = getRacingGame(new JoinCode(joinCode));

        processDescription(joinCode, racingGame);

        eventPublisher.publishEvent(RaceStateChangedEvent.of(racingGame, joinCode));
        log.info("레이싱 게임 시작 완료: joinCode={}", joinCode);
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.RACING_GAME;
    }

    public void tap(String joinCode, String playerName, int tapCount) {
        final RacingGame racingGame = getRacingGame(new JoinCode(joinCode));
        racingGame.updateSpeed(playerName, tapCount, speedCalculator, Instant.now());

        log.debug("탭 처리 완료: joinCode={}, playerName={}, tapCount={}", joinCode, playerName, tapCount);
    }

    private void startAutoMove(RacingGame racingGame, String joinCode) {
        racingGame.updateState(RacingGameState.PLAYING);
        eventPublisher.publishEvent(RaceStateChangedEvent.of(racingGame, joinCode));
        racingGame.setUpStart();
        final ScheduledFuture<?> autoMoveFuture = scheduleAutoMoveTask(racingGame, joinCode);
        racingGame.setAutoMoveFuture(autoMoveFuture);
    }

    private void processDescription(String joinCode, RacingGame racingGame) {
        racingGame.updateState(RacingGameState.DESCRIPTION);
        taskScheduler.schedule(() -> {
            processPrepare(racingGame, joinCode);
            eventPublisher.publishEvent(RaceStateChangedEvent.of(racingGame, joinCode));
        }, Instant.now().plus(timing.description()));
    }

    private void processPrepare(RacingGame racingGame, String joinCode) {
        racingGame.updateState(RacingGameState.PREPARE);
        eventPublisher.publishEvent(RunnersMovedEvent.of(racingGame, joinCode));
        taskScheduler.schedule(() -> startAutoMove(racingGame, joinCode),
                Instant.now().plus(timing.prepare()));
    }

    private ScheduledFuture<?> scheduleAutoMoveTask(RacingGame racingGame, String joinCode) {
        return taskScheduler.scheduleAtFixedRate(() -> executeAutoMove(racingGame, joinCode),
                Duration.ofMillis(RacingGame.MOVE_INTERVAL_MILLIS));
    }

    private void executeAutoMove(RacingGame racingGame, String joinCode) {
        try {
            if (!racingGame.isStarted()) {
                return;
            }
            racingGame.moveAll();
            publishRunnersMoved(racingGame, joinCode);

            if (racingGame.isAllStopped()) {
                handleRaceFinished(racingGame, joinCode);
            }
        } catch (Exception e) {
            handleAutoMoveError(racingGame, e);
        }
    }

    private void handleRaceFinished(RacingGame racingGame, String joinCode) {
        racingGame.updateState(RacingGameState.DONE);
        // 순서 불변식(ADR-0025 결정 5): finishGame()으로 roundCount를 먼저 확정·상태 복귀시킨다.
        final int roundCount = gameSessionService.finishGame(new JoinCode(joinCode));
        taskScheduler.schedule(() -> eventPublisher.publishEvent(RaceFinishedEvent.of(racingGame, joinCode)),
                Instant.now().plus(timing.raceFinishedDelay()));
        racingGame.stopAutoMove();
        // 확률 조정·결과 저장을 유발하는 이벤트는 종료 알림·정리를 모두 끝낸 뒤 마지막에 발행한다 —
        // 저장 리스너(@Transactional/@RedisLock) 실패가 게임 종료 알림·자동이동 정지를 막지 않도록.
        eventPublisher.publishEvent(new MiniGameFinishedEvent(
                joinCode, MiniGameType.RACING_GAME.name(), racingGame.getResult().toRankMap(), roundCount));
        log.info("레이싱 게임 종료: joinCode={}", joinCode);
    }

    private void publishRunnersMoved(RacingGame racingGame, String joinCode) {
        eventPublisher.publishEvent(RunnersMovedEvent.of(racingGame, joinCode));
    }

    private void handleAutoMoveError(RacingGame racingGame, Exception e) {
        log.error("자동 이동 중 오류 발생", e);
        racingGame.stopAutoMove();
    }

    private RacingGame getRacingGame(JoinCode joinCode) {
        return (RacingGame) gameSessionService.getSession(joinCode)
                .findCompletedGame(MiniGameType.RACING_GAME);
    }
}
