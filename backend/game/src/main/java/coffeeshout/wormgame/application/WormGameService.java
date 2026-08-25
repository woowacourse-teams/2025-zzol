package coffeeshout.wormgame.application;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.wormgame.config.WormGameTimingProperties;
import coffeeshout.wormgame.domain.WormGame;
import coffeeshout.wormgame.domain.WormGameState;
import coffeeshout.wormgame.domain.event.WormGameStateChangedEvent;
import coffeeshout.wormgame.domain.event.WormSnapshotEvent;
import coffeeshout.wormgame.domain.event.WormsMovedEvent;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

/**
 * 지렁이 게임 흐름 — DESCRIPTION → PREPARE → PLAYING(틱 루프) → FINISH(종료 연출) → DONE.
 * 판정·시간은 전부 도메인 틱에서 나오고, 서비스는 전이·스케줄·이벤트 발행만 조합한다.
 */
@Slf4j
@Service
public class WormGameService implements MiniGameService {

    private final GameSessionService gameSessionService;
    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;
    private final WormGameTimingProperties timing;
    private final long snapshotEveryTicks;
    private final Map<String, ScheduledFuture<?>> tickFutures = new ConcurrentHashMap<>();

    public WormGameService(
            GameSessionService gameSessionService,
            @Qualifier("wormGameScheduler") TaskScheduler taskScheduler,
            ApplicationEventPublisher eventPublisher,
            WormGameTimingProperties timing) {
        this.gameSessionService = gameSessionService;
        this.taskScheduler = taskScheduler;
        this.eventPublisher = eventPublisher;
        this.timing = timing;
        this.snapshotEveryTicks =
                Math.max(1, timing.snapshotInterval().toMillis() / timing.tick().toMillis());
    }

    @Override
    public void start(String joinCode, String hostName) {
        final WormGame game = getWormGame(joinCode);
        changeState(game, joinCode, WormGameState.DESCRIPTION);
        taskScheduler.schedule(() -> prepare(game, joinCode), Instant.now().plus(timing.description()));
        log.info("지렁이 게임 시작: joinCode={}", joinCode);
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.WORM_GAME;
    }

    public void steer(String joinCode, String playerName, double angle, long seq) {
        getWormGame(joinCode).steer(playerName, angle, seq);
    }

    /** 구독 시점 유니캐스트·주기 브로드캐스트가 공유하는 현재 스냅샷. */
    public WormSnapshotEvent snapshot(String joinCode) {
        return WormSnapshotEvent.of(getWormGame(joinCode), joinCode);
    }

    private void prepare(WormGame game, String joinCode) {
        changeState(game, joinCode, WormGameState.PREPARE);
        publishSnapshot(game, joinCode); // 스폰 위치·방향 표시용 초기 배치
        taskScheduler.schedule(() -> play(game, joinCode), Instant.now().plus(timing.prepare()));
    }

    private void play(WormGame game, String joinCode) {
        changeState(game, joinCode, WormGameState.PLAYING);
        tickFutures.put(joinCode, taskScheduler.scheduleAtFixedRate(() -> tick(game, joinCode), timing.tick()));
    }

    private void tick(WormGame game, String joinCode) {
        try {
            if (!game.isPlaying()) {
                return;
            }
            game.tick();
            eventPublisher.publishEvent(WormsMovedEvent.of(game, joinCode));
            if (game.getTickCount() % snapshotEveryTicks == 0) {
                publishSnapshot(game, joinCode);
            }
            if (game.isRoundOver()) {
                finish(game, joinCode);
            }
        } catch (Exception e) {
            log.error("틱 처리 중 오류 — 틱 루프를 중단합니다: joinCode={}", joinCode, e);
            stopTicking(joinCode);
        }
    }

    private void finish(WormGame game, String joinCode) {
        stopTicking(joinCode);
        changeState(game, joinCode, WormGameState.FINISH);
        publishSnapshot(game, joinCode); // 최종 궤적 정지 화면
        taskScheduler.schedule(() -> done(game, joinCode), Instant.now().plus(timing.finish()));
    }

    private void done(WormGame game, String joinCode) {
        game.updateState(WormGameState.DONE);
        // 순서 불변식(ADR-0025 결정 5): finishGame()으로 roundCount를 먼저 확정·상태 복귀시킨다.
        final int roundCount = gameSessionService.finishGame(new JoinCode(joinCode));
        eventPublisher.publishEvent(WormGameStateChangedEvent.of(game, joinCode));
        // 확률 조정·결과 저장을 유발하는 이벤트는 종료 알림을 끝낸 뒤 마지막에 발행한다 —
        // 저장 리스너 실패가 게임 종료 알림을 막지 않도록.
        eventPublisher.publishEvent(new MiniGameFinishedEvent(
                joinCode, MiniGameType.WORM_GAME.name(), game.getResult().toRankMap(), roundCount));
        log.info("지렁이 게임 종료: joinCode={}, ticks={}", joinCode, game.getTickCount());
    }

    private void changeState(WormGame game, String joinCode, WormGameState state) {
        game.updateState(state);
        eventPublisher.publishEvent(WormGameStateChangedEvent.of(game, joinCode));
    }

    private void publishSnapshot(WormGame game, String joinCode) {
        eventPublisher.publishEvent(WormSnapshotEvent.of(game, joinCode));
    }

    private void stopTicking(String joinCode) {
        final ScheduledFuture<?> future = tickFutures.remove(joinCode);
        if (future != null) {
            future.cancel(false);
        }
    }

    private WormGame getWormGame(String joinCode) {
        return (WormGame)
                gameSessionService.getSession(new JoinCode(joinCode)).findCompletedGame(MiniGameType.WORM_GAME);
    }
}
