package coffeeshout.speedtouch.application;

import coffeeshout.global.metric.GameDurationMetricService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.speedtouch.config.SpeedTouchGameTimingProperties;
import coffeeshout.speedtouch.domain.SpeedTouchGame;
import coffeeshout.speedtouch.domain.SpeedTouchGameState;
import coffeeshout.speedtouch.domain.event.SpeedTouchFinishedEvent;
import coffeeshout.speedtouch.domain.event.SpeedTouchProgressEvent;
import coffeeshout.speedtouch.domain.event.SpeedTouchStateChangedEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SpeedTouchGameService implements MiniGameService {

    private final RoomQueryService roomQueryService;
    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;
    private final SpeedTouchGameTimingProperties timing;
    private final GameDurationMetricService gameDurationMetricService;

    public SpeedTouchGameService(
            RoomQueryService roomQueryService,
            @Qualifier("speedTouchGameScheduler") TaskScheduler taskScheduler,
            ApplicationEventPublisher eventPublisher,
            SpeedTouchGameTimingProperties timing,
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
        final SpeedTouchGame game = getSpeedTouchGame(room);

        scheduleDescription(game, joinCode);

        eventPublisher.publishEvent(SpeedTouchStateChangedEvent.of(game, joinCode));
        log.info("스피드 터치 게임 시작: joinCode={}", joinCode);
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.SPEED_TOUCH;
    }

    private void scheduleDescription(SpeedTouchGame game, String joinCode) {
        game.updateState(SpeedTouchGameState.DESCRIPTION);
        taskScheduler.schedule(
                () -> schedulePrepare(game, joinCode),
                Instant.now().plus(timing.description().toMillis(), ChronoUnit.MILLIS)
        );
    }

    private void schedulePrepare(SpeedTouchGame game, String joinCode) {
        game.updateState(SpeedTouchGameState.PREPARE);
        eventPublisher.publishEvent(SpeedTouchStateChangedEvent.of(game, joinCode));
        eventPublisher.publishEvent(SpeedTouchProgressEvent.of(game, joinCode));

        taskScheduler.schedule(
                () -> startPlaying(game, joinCode),
                Instant.now().plus(timing.prepare().toMillis(), ChronoUnit.MILLIS)
        );
    }

    private void startPlaying(SpeedTouchGame game, String joinCode) {
        game.startPlaying();
        eventPublisher.publishEvent(SpeedTouchStateChangedEvent.of(game, joinCode));
        gameDurationMetricService.startGameTimer(joinCode);

        final ScheduledFuture<?> timeoutFuture = taskScheduler.schedule(
                () -> handleTimeout(game, joinCode),
                Instant.now().plus(timing.playing().toMillis(), ChronoUnit.MILLIS)
        );
        game.setTimeoutFuture(timeoutFuture);
    }

    private void handleTimeout(SpeedTouchGame game, String joinCode) {
        if (game.isDone()) {
            return;
        }
        log.info("스피드 터치 게임 타임아웃: joinCode={}", joinCode);
        finishGame(game, joinCode);
    }

    void finishGame(SpeedTouchGame game, String joinCode) {
        game.updateState(SpeedTouchGameState.DONE);
        game.cancelTimeout();

        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        room.applyMiniGameResult(game.getResult());

        eventPublisher.publishEvent(SpeedTouchProgressEvent.of(game, joinCode));
        taskScheduler.schedule(
                () -> eventPublisher.publishEvent(SpeedTouchFinishedEvent.of(game, joinCode)),
                Instant.now().plusSeconds(2)
        );
        eventPublisher.publishEvent(new MiniGameFinishedEvent(joinCode, MiniGameType.SPEED_TOUCH.name()));
        log.info("스피드 터치 게임 종료: joinCode={}", joinCode);
    }

    SpeedTouchGame getSpeedTouchGame(Room room) {
        return (SpeedTouchGame) room.findMiniGame(MiniGameType.SPEED_TOUCH);
    }
}
