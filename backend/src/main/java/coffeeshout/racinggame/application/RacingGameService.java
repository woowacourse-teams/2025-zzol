package coffeeshout.racinggame.application;

import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.RacingGameState;
import coffeeshout.racinggame.domain.SpeedCalculator;
import coffeeshout.racinggame.domain.event.RaceFinishedEvent;
import coffeeshout.racinggame.domain.event.RaceStateChangedEvent;
import coffeeshout.racinggame.domain.event.RunnersMovedEvent;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomQueryService;
import java.time.Duration;
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
public class RacingGameService implements MiniGameService {

    private final RoomQueryService roomQueryService;
    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;
    private final SpeedCalculator speedCalculator;

    public RacingGameService(
            RoomQueryService roomQueryService,
            @Qualifier("racingGameScheduler") TaskScheduler taskScheduler,
            ApplicationEventPublisher eventPublisher,
            SpeedCalculator speedCalculator
    ) {
        this.roomQueryService = roomQueryService;
        this.taskScheduler = taskScheduler;
        this.eventPublisher = eventPublisher;
        this.speedCalculator = speedCalculator;
    }

    @Override
    public void start(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final RacingGame racingGame = getRacingGame(room);

        processDescription(joinCode, racingGame);

        eventPublisher.publishEvent(RaceStateChangedEvent.of(racingGame, joinCode));
        log.info("레이싱 게임 시작 완료: joinCode={}", joinCode);
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.RACING_GAME;
    }

    public void tap(String joinCode, String playerName, int tapCount) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final RacingGame racingGame = getRacingGame(room);
        final Player player = room.findPlayer(new PlayerName(playerName));
        racingGame.updateSpeed(player, tapCount, speedCalculator, Instant.now());

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
        }, Instant.now().plus(racingGame.getState().getDuration(), ChronoUnit.MILLIS));
    }

    private void processPrepare(RacingGame racingGame, String joinCode) {
        racingGame.updateState(RacingGameState.PREPARE);
        eventPublisher.publishEvent(RunnersMovedEvent.of(racingGame, joinCode));
        taskScheduler.schedule(() -> startAutoMove(racingGame, joinCode),
                Instant.now().plus(racingGame.getState().getDuration(), ChronoUnit.MILLIS));
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
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        room.applyMiniGameResult(racingGame.getResult());
        taskScheduler.schedule(() -> eventPublisher.publishEvent(RaceFinishedEvent.of(racingGame, joinCode)),
                Instant.now().plusSeconds(2));
        eventPublisher.publishEvent(new MiniGameFinishedEvent(joinCode, MiniGameType.RACING_GAME.name()));
        racingGame.stopAutoMove();
        log.info("레이싱 게임 종료: joinCode={}", joinCode);
    }

    private void publishRunnersMoved(RacingGame racingGame, String joinCode) {
        eventPublisher.publishEvent(RunnersMovedEvent.of(racingGame, joinCode));
    }

    private void handleAutoMoveError(RacingGame racingGame, Exception e) {
        log.error("자동 이동 중 오류 발생", e);
        racingGame.stopAutoMove();
    }

    private RacingGame getRacingGame(Room room) {
        return (RacingGame) room.findMiniGame(MiniGameType.RACING_GAME);
    }
}
