package coffeeshout.nunchi.application;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.nunchi.config.NunchiTimingProperties;
import coffeeshout.nunchi.domain.NunchiGame;
import coffeeshout.nunchi.domain.PressResult;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

/**
 * 눈치게임 Flow(ADR-0031 — 난이도 최상). 현 {@code FlowScheduler} SPI는 단발·취소불가라 윈도우(300ms)·
 * 쿨다운·idle·하드캡을 동적으로 cancel·reschedule할 수 없다(N2). 따라서 raw {@link TaskScheduler}를
 * 직접 주입하고 joinCode별 {@link NunchiSession}에 4개 타이머 future를 들어 미세 제어한다.
 *
 * <p><b>동시성</b>: press는 {@code [nunchi]} 스트림 단일스레드 풀에서, 타이머 콜백은
 * {@code nunchiGameScheduler} 풀에서 도는 <b>서로 다른 스레드</b>다. 컨슈머 단일스레드만으로는 press와
 * 타이머 콜백 사이가 직렬화되지 않으므로, 모든 게임 상태 변경을 joinCode별 {@code session.lock}으로 묶는다.
 * {@code future.cancel(false)}는 best-effort라 막 발화한 콜백은 그대로 실행되므로, 모든 콜백은 락 안에서
 * {@code session.finished}를 재확인한다.
 *
 * <p><b>종료 멱등성</b>: 전원 입력·idle·하드캡 세 경로가 거의 동시에 발화할 수 있다. {@link NunchiGame}은
 * {@code tryFinish} 가드가 없으므로(SpeedTouch와 달리) {@code session.finished} 플래그로 단 한 번만
 * {@code finishByTimeout}+DONE+{@code MiniGameFinishedEvent}를 수행한다(중복 round-count/결과 저장 방지).
 */
@Slf4j
@Service
public class NunchiFlowOrchestrator {

    private final TaskScheduler taskScheduler;
    private final NunchiTimingProperties timing;
    private final NunchiNotifier notifier;
    private final GameSessionService gameSessionService;
    private final ApplicationEventPublisher eventPublisher;

    private final ConcurrentHashMap<String, NunchiSession> sessions = new ConcurrentHashMap<>();

    public NunchiFlowOrchestrator(
            @Qualifier("nunchiGameScheduler") TaskScheduler taskScheduler,
            NunchiTimingProperties timing,
            NunchiNotifier notifier,
            GameSessionService gameSessionService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.taskScheduler = taskScheduler;
        this.timing = timing;
        this.notifier = notifier;
        this.gameSessionService = gameSessionService;
        this.eventPublisher = eventPublisher;
    }

    public void startFlow(NunchiGame game, JoinCode joinCode) {
        final String code = joinCode.getValue();
        final NunchiSession session = new NunchiSession();
        sessions.put(code, session);

        synchronized (session.lock) {
            final long now = System.currentTimeMillis();
            final long idleDeadline = now + timing.idleTimeout().toMillis();
            final long hardCap = now + timing.hardCap().toMillis();

            notifier.notifyPlaying(code, game.getCurrentNumber(), snapshotStood(session), now, idleDeadline, hardCap);

            session.idle = schedule(() -> onTimeout(game, code, "idle"), timing.idleTimeout());
            session.hardCap = schedule(() -> onTimeout(game, code, "hardCap"), timing.hardCap());
        }
    }

    /** press 입력 — 도메인 판정 후 결과에 따라 타이머·브로드캐스트를 joinCode 락 아래에서 처리한다. */
    public void handlePress(NunchiGame game, JoinCode joinCode, Gamer gamer, Instant at) {
        final String code = joinCode.getValue();
        final NunchiSession session = sessions.get(code);
        if (session == null) {
            log.warn("눈치게임 세션 없음 — press 무시: joinCode={}, player={}", code, gamer.getName());
            return;
        }
        synchronized (session.lock) {
            if (session.finished) {
                return;
            }
            final PressResult result = game.press(gamer, at);
            switch (result.outcome()) {
                case STOOD -> onStood(game, code, session, gamer);
                case COLLIDED -> onCollided(game, code, session, result);
                case IGNORED -> log.warn("눈치게임 press 무시(이미 입력/쿨다운 밖/종료): joinCode={}, player={}",
                        code, gamer.getName());
            }
        }
    }

    private void onStood(NunchiGame game, String code, NunchiSession session, Gamer gamer) {
        session.stood.add(gamer.getName());
        final long now = System.currentTimeMillis();
        final long idleDeadline = now + timing.idleTimeout().toMillis();

        notifier.notifyStood(code, gamer.getName(), game.getCurrentNumber(), now, idleDeadline);

        // 윈도우 타이머 재예약: 이 번호가 윈도우 안에 또 눌리지 않으면 solo 확정(N2)
        reschedule(session, Timer.WINDOW, () -> onWindowClose(game, code), timing.numberWindow());
        // 유효 입력이므로 idle 리셋(N6)
        reschedule(session, Timer.IDLE, () -> onTimeout(game, code, "idle"), timing.idleTimeout());

        finishIfAllPressed(game, code, session);
    }

    private void onCollided(NunchiGame game, String code, NunchiSession session, PressResult result) {
        final List<String> collided = result.collidedGroup().stream().map(Gamer::getName).toList();
        session.stood.removeAll(collided); // 충돌자는 깨끗이 선 사람이 아니므로 stood 스냅샷에서 제거

        cancel(session, Timer.WINDOW); // 충돌 확정 — 이 번호의 윈도우는 끝
        cancel(session, Timer.IDLE);   // 쿨다운 동안 idle 일시정지(N6, 쿨다운을 idle로 오인 방지)

        final long now = System.currentTimeMillis();
        final long resumeAt = now + timing.collisionCooldown().toMillis();
        notifier.notifyCollisionCooldown(code, result.number(), collided, now, resumeAt);

        reschedule(session, Timer.COOLDOWN, () -> onCooldownEnd(game, code), timing.collisionCooldown());

        // 2명이 서로 충돌해 남는 사람이 없으면 즉시 종료(N3 — 무한 쿨다운 방지)
        finishIfAllPressed(game, code, session);
    }

    private void onWindowClose(NunchiGame game, String code) {
        final NunchiSession session = sessions.get(code);
        if (session == null) {
            return;
        }
        synchronized (session.lock) {
            if (session.finished) {
                return;
            }
            game.closeWindow(); // pending이 살아있으면 solo 확정, 카운터 전진
            broadcastPlaying(game, code, session);
            finishIfAllPressed(game, code, session);
        }
    }

    private void onCooldownEnd(NunchiGame game, String code) {
        final NunchiSession session = sessions.get(code);
        if (session == null) {
            return;
        }
        synchronized (session.lock) {
            if (session.finished) {
                return;
            }
            game.endCooldown(); // 같은 번호로 PLAYING 재개
            broadcastPlaying(game, code, session);
            // idle 재개(N6)
            reschedule(session, Timer.IDLE, () -> onTimeout(game, code, "idle"), timing.idleTimeout());
        }
    }

    private void onTimeout(NunchiGame game, String code, String reason) {
        final NunchiSession session = sessions.get(code);
        if (session == null) {
            return;
        }
        synchronized (session.lock) {
            if (session.finished) {
                return;
            }
            log.info("눈치게임 타임아웃 종료: joinCode={}, reason={}", code, reason);
            finish(game, code, session);
        }
    }

    /** 전원이 입력을 마쳤으면 즉시 종료(결정 5 조기 종료). 호출자는 락을 보유한 상태여야 한다. */
    private void finishIfAllPressed(NunchiGame game, String code, NunchiSession session) {
        if (game.isAllPressed()) {
            log.info("눈치게임 전원 입력 — 조기 종료: joinCode={}", code);
            finish(game, code, session);
        }
    }

    /** 멱등 종료. 호출자는 락을 보유한 상태여야 한다. */
    private void finish(NunchiGame game, String code, NunchiSession session) {
        if (session.finished) {
            return;
        }
        session.finished = true;
        cancelAll(session);

        game.finishByTimeout(); // pending solo 확정 + 미입력자 MISS + DONE
        notifier.notifyDone(code);

        // 순서 불변식(ADR-0025 결정 5): finishGame()으로 roundCount 확정·상태 복귀 후 이벤트 발행
        final int roundCount = gameSessionService.finishGame(new JoinCode(code));
        eventPublisher.publishEvent(new MiniGameFinishedEvent(
                code, MiniGameType.NUNCHI_GAME.name(), game.getResult().toRankMap(), roundCount));

        sessions.remove(code);
    }

    private void broadcastPlaying(NunchiGame game, String code, NunchiSession session) {
        final long now = System.currentTimeMillis();
        final long idleDeadline = now + timing.idleTimeout().toMillis();
        final long hardCap = now + timing.hardCap().toMillis();
        notifier.notifyPlaying(code, game.getCurrentNumber(), snapshotStood(session), now, idleDeadline, hardCap);
    }

    private List<String> snapshotStood(NunchiSession session) {
        return new ArrayList<>(session.stood);
    }

    private ScheduledFuture<?> schedule(Runnable task, Duration delay) {
        return taskScheduler.schedule(task, Instant.now().plus(delay));
    }

    private void reschedule(NunchiSession session, Timer timer, Runnable task, Duration delay) {
        cancel(session, timer);
        final ScheduledFuture<?> future = schedule(task, delay);
        switch (timer) {
            case WINDOW -> session.window = future;
            case IDLE -> session.idle = future;
            case COOLDOWN -> session.cooldown = future;
            case HARD_CAP -> session.hardCap = future;
        }
    }

    private void cancel(NunchiSession session, Timer timer) {
        switch (timer) {
            case WINDOW -> session.window = cancel(session.window);
            case IDLE -> session.idle = cancel(session.idle);
            case COOLDOWN -> session.cooldown = cancel(session.cooldown);
            case HARD_CAP -> session.hardCap = cancel(session.hardCap);
        }
    }

    private void cancelAll(NunchiSession session) {
        session.window = cancel(session.window);
        session.idle = cancel(session.idle);
        session.cooldown = cancel(session.cooldown);
        session.hardCap = cancel(session.hardCap);
    }

    private ScheduledFuture<?> cancel(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(false);
        }
        return null;
    }

    private enum Timer {WINDOW, IDLE, COOLDOWN, HARD_CAP}

    /** joinCode별 Flow 상태. 모든 필드 접근은 {@code lock} 아래에서만 한다. */
    private static final class NunchiSession {
        private final Object lock = new Object();
        private final List<String> stood = new ArrayList<>();
        private ScheduledFuture<?> window;
        private ScheduledFuture<?> idle;
        private ScheduledFuture<?> cooldown;
        private ScheduledFuture<?> hardCap;
        private boolean finished;
    }
}
