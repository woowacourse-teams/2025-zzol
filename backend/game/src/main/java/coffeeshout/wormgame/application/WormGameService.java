package coffeeshout.wormgame.application;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.wormgame.config.WormGameRulesProperties;
import coffeeshout.wormgame.config.WormGameTimingProperties;
import coffeeshout.wormgame.domain.WormGame;
import coffeeshout.wormgame.domain.WormGameState;
import coffeeshout.wormgame.domain.event.WormGameStateChangedEvent;
import coffeeshout.wormgame.domain.event.WormSnapshotEvent;
import coffeeshout.wormgame.domain.event.WormsMovedEvent;
import java.time.Duration;
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
 *
 * <p>도메인 상태는 게임 인스턴스 단위 락으로 보호한다 — 틱 스레드가 궤적을 append하는 동안 구독 시점
 * 스냅샷(WS inbound 스레드)이 같은 리스트를 읽으면 안 된다. 틱은 수십 μs라 락 경합은 무시할 수준이다.
 * ponytail: 게임 단위 global lock, 틱이 무거워지면 틱 스레드가 만든 스냅샷을 캐싱하는 방식으로 전환.
 */
@Slf4j
@Service
public class WormGameService implements MiniGameService {

    private final GameSessionService gameSessionService;
    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;
    private final WormGameTimingProperties timing;
    /** 틱 주기의 단일 출처는 도메인 규칙의 tickMillis다 — 점수(생존 ms)와 스케줄 주기가 어긋나지 않도록. */
    private final Duration tickPeriod;

    private final long snapshotEveryTicks;
    private final Map<String, ScheduledFuture<?>> tickFutures = new ConcurrentHashMap<>();

    public WormGameService(
            GameSessionService gameSessionService,
            @Qualifier("wormGameScheduler") TaskScheduler taskScheduler,
            ApplicationEventPublisher eventPublisher,
            WormGameTimingProperties timing,
            WormGameRulesProperties rules) {
        this.gameSessionService = gameSessionService;
        this.taskScheduler = taskScheduler;
        this.eventPublisher = eventPublisher;
        this.timing = timing;
        this.tickPeriod = Duration.ofMillis(rules.tickMillis());
        this.snapshotEveryTicks = Math.max(1, timing.snapshotInterval().toMillis() / rules.tickMillis());
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

    /** 구독 시점 유니캐스트가 쓰는 현재 스냅샷 — 틱과 상호배제한 채 만든다. */
    public WormSnapshotEvent snapshot(String joinCode) {
        final WormGame game = getWormGame(joinCode);
        synchronized (game) {
            return WormSnapshotEvent.of(game, joinCode);
        }
    }

    private void prepare(WormGame game, String joinCode) {
        changeState(game, joinCode, WormGameState.PREPARE);
        publishSnapshot(game, joinCode); // 스폰 위치·방향 표시용 초기 배치
        taskScheduler.schedule(() -> play(game, joinCode), Instant.now().plus(timing.prepare()));
    }

    private void play(WormGame game, String joinCode) {
        changeState(game, joinCode, WormGameState.PLAYING);
        // 첫 틱을 한 주기 뒤로 미룬다 — 초기 지연 0이면 첫 틱이 다른 풀 스레드에서 put보다 먼저 끝나
        // finish/예외 경로의 stopTicking이 future를 못 찾고, 취소되지 않은 루프가 남는다.
        tickFutures.put(
                joinCode,
                taskScheduler.scheduleAtFixedRate(
                        () -> tick(game, joinCode), Instant.now().plus(tickPeriod), tickPeriod));
    }

    private void tick(WormGame game, String joinCode) {
        try {
            synchronized (game) {
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
            }
        } catch (Exception e) {
            // 루프만 끊으면 상태가 PLAYING 에 머물러 세션이 영원히 안 풀린다 — 클라는 DONE 에서만
            // 결과로 넘어가고, 게임 세션이 잠기면 룰렛 진행까지 막힌다. 알림이 실패해도 종료는 시킨다.
            log.error("틱 처리 중 오류 — 틱 루프를 중단하고 게임을 종료합니다: joinCode={}", joinCode, e);
            stopTicking(joinCode);
            abort(game, joinCode);
        }
    }

    private void finish(WormGame game, String joinCode) {
        stopTicking(joinCode);
        changeState(game, joinCode, WormGameState.FINISH);
        publishSnapshot(game, joinCode); // 최종 궤적 정지 화면
        taskScheduler.schedule(() -> done(game, joinCode), Instant.now().plus(timing.finish()));
    }

    /** 틱 예외 복구 — 남은 연출 없이 FINISH 로 표시하고 정상 종료 경로(done)로 합류시킨다. */
    private void abort(WormGame game, String joinCode) {
        try {
            changeState(game, joinCode, WormGameState.FINISH);
        } catch (Exception e) {
            log.error("종료 상태 알림 실패 — done 스케줄은 그대로 진행합니다: joinCode={}", joinCode, e);
        }
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
        synchronized (game) {
            eventPublisher.publishEvent(WormSnapshotEvent.of(game, joinCode));
        }
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
