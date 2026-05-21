package coffeeshout.blindtimer.application;

import coffeeshout.blindtimer.config.BlindTimerGameTimingProperties;
import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blindtimer.domain.BlindTimerGameState;
import coffeeshout.blindtimer.domain.event.BlindTimerFinishedEvent;
import coffeeshout.blindtimer.domain.event.BlindTimerProgressEvent;
import coffeeshout.blindtimer.domain.event.BlindTimerStateChangedEvent;
import coffeeshout.gamecommon.metric.GameDurationMetricService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.application.service.RoomQueryService;
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

    private final RoomQueryService roomQueryService;
    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;
    private final BlindTimerGameTimingProperties timing;
    private final GameDurationMetricService gameDurationMetricService;

    public BlindTimerGameService(
            RoomQueryService roomQueryService,
            @Qualifier("blindTimerGameScheduler") TaskScheduler taskScheduler,
            ApplicationEventPublisher eventPublisher,
            BlindTimerGameTimingProperties timing,
            GameDurationMetricService gameDurationMetricService
    ) {
        this.roomQueryService = roomQueryService;
        this.taskScheduler = taskScheduler;
        this.eventPublisher = eventPublisher;
        this.timing = timing;
        this.gameDurationMetricService = gameDurationMetricService;
    }

    @Override
    public void start(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final BlindTimerGame game = getBlindTimerGame(room);

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

        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        room.applyMiniGameResult(game.getResult());

        eventPublisher.publishEvent(BlindTimerProgressEvent.of(game, joinCode));
        taskScheduler.schedule(
                () -> eventPublisher.publishEvent(BlindTimerFinishedEvent.of(game, joinCode)),
                Instant.now().plus(timing.resultDelay())
        );
        eventPublisher.publishEvent(new MiniGameFinishedEvent(joinCode, MiniGameType.BLIND_TIMER.name()));
        log.info("블라인드 타이머 게임 종료: joinCode={}", joinCode);
    }

    public BlindTimerGame getBlindTimerGame(Room room) {
        return (BlindTimerGame) room.findMiniGame(MiniGameType.BLIND_TIMER);
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
