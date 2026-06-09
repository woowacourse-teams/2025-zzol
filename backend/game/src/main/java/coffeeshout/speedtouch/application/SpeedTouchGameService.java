package coffeeshout.speedtouch.application;

import coffeeshout.game.metric.GameDurationMetricService;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.domain.Room;
import coffeeshout.room.application.service.RoomQueryService;
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
    private final GameSessionService gameSessionService;
    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;
    private final SpeedTouchGameTimingProperties timing;
    private final GameDurationMetricService gameDurationMetricService;

    public SpeedTouchGameService(
            RoomQueryService roomQueryService,
            GameSessionService gameSessionService,
            @Qualifier("speedTouchGameScheduler") TaskScheduler taskScheduler,
            ApplicationEventPublisher eventPublisher,
            SpeedTouchGameTimingProperties timing,
            GameDurationMetricService gameDurationMetricService
    ) {
        this.roomQueryService = roomQueryService;
        this.gameSessionService = gameSessionService;
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
        log.info("스피드 터치 게임 타임아웃: joinCode={}", joinCode);
        finishGame(game, joinCode);
    }

    public void finishGame(SpeedTouchGame game, String joinCode) {
        if (!game.tryFinish()) {
            return;
        }
        game.cancelTimeout();

        // 순서 불변식(ADR-0023 결정 5): finishGame()으로 roundCount를 먼저 확정·상태 복귀시킨다.
        final int roundCount = gameSessionService.finishGame(new JoinCode(joinCode));

        eventPublisher.publishEvent(SpeedTouchProgressEvent.of(game, joinCode));
        taskScheduler.schedule(
                () -> eventPublisher.publishEvent(SpeedTouchFinishedEvent.of(game, joinCode)),
                Instant.now().plusSeconds(2)
        );
        // 확률 조정·결과 저장을 유발하는 이벤트는 종료 알림을 모두 보낸 뒤 마지막에 발행한다 —
        // 저장 리스너(@Transactional/@RedisLock) 실패가 게임 종료 알림을 막지 않도록(다른 게임과 동일 순서).
        eventPublisher.publishEvent(new MiniGameFinishedEvent(
                joinCode, MiniGameType.SPEED_TOUCH.name(), game.getResult().toRankMap(), roundCount));
        log.info("스피드 터치 게임 종료: joinCode={}", joinCode);
    }

    public SpeedTouchGame getSpeedTouchGame(Room room) {
        return (SpeedTouchGame) gameSessionService.getSession(room.getJoinCode())
                .findCompletedGame(MiniGameType.SPEED_TOUCH);
    }
}
