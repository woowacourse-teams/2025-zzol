package coffeeshout.bombrelay.application;

import coffeeshout.bombrelay.config.BombRelayGameTimingProperties;
import coffeeshout.bombrelay.domain.BombRelayGame;
import coffeeshout.bombrelay.domain.BombRelayGameState;
import coffeeshout.bombrelay.domain.event.BombRelayFinishedEvent;
import coffeeshout.bombrelay.domain.event.BombRelayProgressEvent;
import coffeeshout.bombrelay.domain.event.BombRelayStateChangedEvent;
import coffeeshout.global.metric.GameDurationMetricService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.service.RoomQueryService;
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
public class BombRelayGameService implements MiniGameService {

    private final RoomQueryService roomQueryService;
    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;
    private final BombRelayGameTimingProperties timing;
    private final GameDurationMetricService gameDurationMetricService;

    public BombRelayGameService(
            RoomQueryService roomQueryService,
            @Qualifier("bombRelayGameScheduler") TaskScheduler taskScheduler,
            ApplicationEventPublisher eventPublisher,
            BombRelayGameTimingProperties timing,
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
        final BombRelayGame game = getBombRelayGame(room);

        scheduleDescription(game, joinCode);

        eventPublisher.publishEvent(BombRelayStateChangedEvent.of(game, joinCode));
        log.info("폭탄 끝말잇기 게임 시작: joinCode={}, maxRounds={}", joinCode, game.getMaxRounds());
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.BOMB_RELAY;
    }

    public void startNextRound(BombRelayGame game, String joinCode) {
        game.startRound();
        game.startPlaying();

        eventPublisher.publishEvent(BombRelayStateChangedEvent.of(game, joinCode));
        eventPublisher.publishEvent(BombRelayProgressEvent.of(game, joinCode));

        scheduleBombTimer(game, joinCode);
        log.info("폭탄 끝말잇기 라운드 시작: joinCode={}, round={}/{}, startWord={}",
                joinCode, game.getCurrentRound(), game.getMaxRounds(), game.getCurrentWord());
    }

    public void handleBombExploded(BombRelayGame game, String joinCode) {
        game.cancelBombTimer();
        final String eliminatedName = game.getCurrentTurnPlayer().getName();
        game.eliminateCurrentPlayer();

        log.info("폭탄 폭발! 탈락: joinCode={}, round={}, player={}",
                joinCode, game.getCurrentRound(), eliminatedName);

        eventPublisher.publishEvent(BombRelayProgressEvent.of(game, joinCode));

        if (game.isGameOver()) {
            finishGame(game, joinCode);
        } else {
            game.updateState(BombRelayGameState.ROUND_RESULT);
            eventPublisher.publishEvent(BombRelayStateChangedEvent.of(game, joinCode));

            taskScheduler.schedule(
                    () -> startNextRound(game, joinCode),
                    Instant.now().plus(timing.resultDelay())
            );
        }
    }

    public void resetBombTimer(BombRelayGame game, String joinCode) {
        // 턴이 넘어갈 때마다 폭탄 타이머를 리셋하지 않는다.
        // 폭탄 타이머는 라운드 시작 시 한 번만 설정되고, 라운드 끝날 때까지 유지된다.
        // 이게 핵심: 언제 터질지 모르니까 긴장감이 생긴다.
    }

    public BombRelayGame getBombRelayGame(Room room) {
        return (BombRelayGame) room.findMiniGame(MiniGameType.BOMB_RELAY);
    }

    private void finishGame(BombRelayGame game, String joinCode) {
        if (!game.tryFinish()) {
            return;
        }

        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        room.applyMiniGameResult(game.getResult());

        eventPublisher.publishEvent(BombRelayProgressEvent.of(game, joinCode));
        taskScheduler.schedule(
                () -> eventPublisher.publishEvent(BombRelayFinishedEvent.of(game, joinCode)),
                Instant.now().plus(timing.resultDelay())
        );
        eventPublisher.publishEvent(new MiniGameFinishedEvent(joinCode, MiniGameType.BOMB_RELAY.name()));
        log.info("폭탄 끝말잇기 게임 종료: joinCode={}", joinCode);
    }

    private void scheduleDescription(BombRelayGame game, String joinCode) {
        game.updateState(BombRelayGameState.DESCRIPTION);
        taskScheduler.schedule(
                () -> schedulePrepare(game, joinCode),
                Instant.now().plus(timing.description())
        );
    }

    private void schedulePrepare(BombRelayGame game, String joinCode) {
        game.updateState(BombRelayGameState.PREPARE);
        eventPublisher.publishEvent(BombRelayStateChangedEvent.of(game, joinCode));
        eventPublisher.publishEvent(BombRelayProgressEvent.of(game, joinCode));

        taskScheduler.schedule(
                () -> startNextRound(game, joinCode),
                Instant.now().plus(timing.prepare())
        );
    }

    private void scheduleBombTimer(BombRelayGame game, String joinCode) {
        final Duration bombDuration = timing.randomBombDuration();
        final ScheduledFuture<?> bombFuture = taskScheduler.schedule(
                () -> handleBombExploded(game, joinCode),
                Instant.now().plus(bombDuration)
        );
        game.setBombTimerFuture(bombFuture);
        log.debug("폭탄 타이머 설정: joinCode={}, round={}, duration={}ms",
                joinCode, game.getCurrentRound(), bombDuration.toMillis());
    }
}
