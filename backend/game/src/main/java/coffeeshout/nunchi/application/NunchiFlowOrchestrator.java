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
 * 직접 주입하고 joinCode별 {@link NunchiSession}에 5개 타이머 future(설명·윈도우·idle·쿨다운·하드캡)를
 * 들어 미세 제어한다.
 *
 * <p><b>동시성</b>: press는 {@code [nunchi]} 스트림 단일스레드 풀에서, 타이머 콜백은
 * {@code nunchiGameScheduler} 풀에서 도는 <b>서로 다른 스레드</b>다. 컨슈머 단일스레드만으로는 press와
 * 타이머 콜백 사이가 직렬화되지 않으므로, 모든 게임 상태 변경을 joinCode별 {@code session.lock}으로 묶는다.
 *
 * <p><b>stale 타이머 가드(★)</b>: {@code future.cancel(false)}는 이미 발화해 락을 기다리는 콜백을 막지
 * 못한다 — 대체된(reschedule된) idle/window 콜백이 락을 얻으면 옛 효과를 그대로 실행해 게임을 조기 종료하거나
 * (idle) pending을 오확정한다(window). {@code session.finished}만으론 못 막으므로, 타이머마다
 * <b>generation</b>을 두고 schedule 시점 값을 콜백에 캡처해 발화 시 현재 generation과 일치할 때만 효과를
 * 실행한다(불일치=대체됨=무시).
 *
 * <p><b>종료 멱등성</b>: 전원 입력·idle·하드캡 세 경로가 거의 동시에 발화할 수 있다. {@link NunchiGame}은
 * {@code tryFinish} 가드가 없으므로 {@code session.finished} 플래그로 단 한 번만 {@code finishByTimeout}+
 * DONE+{@code MiniGameFinishedEvent}를 수행한다.
 *
 * <p><b>고정 데드라인</b>: {@code hardCapEpochMs}는 시작 시 한 번 고정한다(결정 8 — 고정 상한).
 * {@code idleDeadlineEpochMs}는 idle 타이머를 (재)예약하는 곳에서 같은 값으로 저장해, 브로드캐스트가 실제
 * 발화 시각과 어긋나지 않게 한다(now+timeout 즉석 계산 금지 — 클라 카운트다운 드리프트 원인).
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
            // 규칙 설명 단계(다른 미니게임과 동일). 이 구간엔 idle·하드캡·윈도우 타이머를 걸지 않고
            // press도 도메인이 거부한다. description이 끝나면 onDescriptionEnd가 PLAYING으로 전이한다.
            final long now = System.currentTimeMillis();
            final long playStartEpochMs = now + timing.description().toMillis();
            // 알림은 격리한다 — 던지면 아래 description 타이머가 안 걸려 종료 경로 없는 DESCRIPTION에 영구 고착된다.
            notifyQuietly(() -> notifier.notifyDescription(code, now, playStartEpochMs));
            session.description = schedule(() -> onDescriptionEnd(game, code), timing.description());
        }
    }

    /** 규칙 설명 종료 — PLAYING으로 전이하고 idle·하드캡 타이머를 건다(시작 시 한 번, 재예약 없음). */
    private void onDescriptionEnd(NunchiGame game, String code) {
        final NunchiSession session = sessions.get(code);
        if (session == null) {
            return;
        }
        synchronized (session.lock) {
            if (session.finished) {
                return;
            }
            game.startPlaying();
            // 하드캡은 PLAYING 시작 시점부터 잰다(설명 시간은 라운드 상한에 포함하지 않음 — 결정 8 고정 상한).
            final long now = System.currentTimeMillis();
            session.hardCapEpochMs = now + timing.hardCap().toMillis();
            scheduleIdle(game, code, session); // idleDeadlineEpochMs 저장 포함 — notify 전에 걸어 종료 경로 보장

            notifyQuietly(() -> notifier.notifyPlaying(code, game.getCurrentNumber(), snapshotStood(session),
                    now, session.idleDeadlineEpochMs, session.hardCapEpochMs));

            session.hardCap = schedule(() -> onHardCap(game, code), timing.hardCap());
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
        scheduleIdle(game, code, session);   // 유효 입력이므로 idle 리셋(N6) — idleDeadlineEpochMs 갱신
        scheduleWindow(game, code, session); // 이 번호가 윈도우 안에 또 눌리지 않으면 solo 확정(N2)

        notifyQuietly(() -> notifier.notifyStood(code, gamer.getName(), game.getCurrentNumber(),
                System.currentTimeMillis(), session.idleDeadlineEpochMs));

        finishIfAllPressed(game, code, session);
    }

    private void onCollided(NunchiGame game, String code, NunchiSession session, PressResult result) {
        final List<String> collided = result.collidedGroup().stream().map(Gamer::getName).toList();
        session.stood.removeAll(collided); // 충돌자는 깨끗이 선 사람이 아니므로 stood 스냅샷에서 제거

        cancelWindow(session); // 충돌 확정 — 이 번호의 윈도우는 끝
        cancelIdle(session);   // 쿨다운 동안 idle 일시정지(N6, 쿨다운을 idle로 오인 방지)

        final long now = System.currentTimeMillis();
        final long resumeAt = now + timing.collisionCooldown().toMillis();
        notifyQuietly(() -> notifier.notifyCollisionCooldown(code, result.number(), collided, now, resumeAt));

        session.cooldown = cancel(session.cooldown);
        session.cooldown = schedule(() -> onCooldownEnd(game, code), timing.collisionCooldown());

        // 2명이 서로 충돌해 남는 사람이 없으면 즉시 종료(N3 — 무한 쿨다운 방지)
        finishIfAllPressed(game, code, session);
    }

    private void onWindowClose(NunchiGame game, String code, long gen) {
        final NunchiSession session = sessions.get(code);
        if (session == null) {
            return;
        }
        synchronized (session.lock) {
            if (session.finished || gen != session.windowGen) {
                return; // 종료됐거나 대체된 stale 윈도우 — 무시
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
            scheduleIdle(game, code, session); // idle 재개(N6) — idleDeadlineEpochMs 갱신
            broadcastPlaying(game, code, session);
        }
    }

    /** idle 타임아웃 — 캡처한 generation이 최신일 때만 종료(대체된 stale idle 무시). */
    private void onIdleTimeout(NunchiGame game, String code, long gen) {
        final NunchiSession session = sessions.get(code);
        if (session == null) {
            return;
        }
        synchronized (session.lock) {
            if (session.finished || gen != session.idleGen) {
                return; // 종료됐거나 대체된 stale idle — 무시
            }
            log.info("눈치게임 idle 타임아웃 종료: joinCode={}", code);
            finish(game, code, session);
        }
    }

    /** 하드캡 — 시작 시 한 번만 예약되고 재예약되지 않으므로 generation 불필요. finished만 확인한다. */
    private void onHardCap(NunchiGame game, String code) {
        final NunchiSession session = sessions.get(code);
        if (session == null) {
            return;
        }
        synchronized (session.lock) {
            if (session.finished) {
                return;
            }
            log.info("눈치게임 하드캡 종료: joinCode={}", code);
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

    /**
     * 멱등 종료. 호출자는 락을 보유한 상태여야 한다. DONE은 즉시 알리되, 다음 단계(SCORE_BOARD 전이)는
     * 결과를 잠깐 보여준 뒤 {@code resultDelay} 후에 넘어간다(결정 9 — Ladder의 RESULT 지연·BlindTimer
     * result-delay와 동일한 "결과 보여주고 전이" 패턴).
     */
    private void finish(NunchiGame game, String code, NunchiSession session) {
        if (session.finished) {
            return;
        }
        session.finished = true;
        cancelAll(session);

        game.finishByTimeout(); // pending solo 확정 + 미입력자 MISS + DONE
        notifyQuietly(() -> notifier.notifyDone(code));

        // DONE 브로드캐스트는 즉시 나가고, 다음 단계로의 전이(roundCount 확정 + MiniGameFinishedEvent 발행)는
        // resultDelay 만큼 미룬다. 종료는 단발(finished 가드)이라 이 타이머는 정확히 한 번 예약된다.
        session.result = schedule(() -> finalizeGame(game, code), timing.resultDelay());
    }

    /**
     * 결과 대기({@code resultDelay}) 후 라운드를 확정하고 다음 단계로 넘긴다.
     *
     * <p>순서 불변식(ADR-0025 결정 5): {@code finishGame()}으로 roundCount 확정·상태 복귀 후
     * {@link MiniGameFinishedEvent}를 발행한다(결과 저장·라운드 전진·확률 조정·SCORE_BOARD 전이 유발). 발행은
     * 동기로, 저장 리스너 실패가 흐름을 막지 않도록 한다 — BlockStacking/SpeedTouch 동일.
     */
    private void finalizeGame(NunchiGame game, String code) {
        final NunchiSession session = sessions.get(code);
        if (session == null) {
            return; // 이미 정리됨
        }
        synchronized (session.lock) {
            final int roundCount = gameSessionService.finishGame(new JoinCode(code));
            eventPublisher.publishEvent(new MiniGameFinishedEvent(
                    code, MiniGameType.NUNCHI_GAME.name(), game.getResult().toRankMap(), roundCount));
            sessions.remove(code);
        }
    }

    private void broadcastPlaying(NunchiGame game, String code, NunchiSession session) {
        notifyQuietly(() -> notifier.notifyPlaying(code, game.getCurrentNumber(), snapshotStood(session),
                System.currentTimeMillis(), session.idleDeadlineEpochMs, session.hardCapEpochMs));
    }

    /**
     * 브로드캐스트 실패(브로커/직렬화 오류)가 흐름·타이머 예약을 막지 않도록 격리한다(Ladder 패턴). 알림은
     * 결과 통지일 뿐 게임 진행의 권위가 아니므로, 실패해도 타이머·상태 전이는 그대로 진행해야 한다.
     */
    private void notifyQuietly(Runnable broadcast) {
        try {
            broadcast.run();
        } catch (Exception e) {
            log.warn("눈치게임 브로드캐스트 실패(흐름은 계속): {}", e.getMessage(), e);
        }
    }

    private List<String> snapshotStood(NunchiSession session) {
        return new ArrayList<>(session.stood);
    }

    // ---- 타이머 (재)예약 — generation 발급 + 절대 데드라인 저장을 한곳에 모은다 ----

    /** idle 타이머를 (재)예약하고 절대 데드라인을 저장한다. generation을 올려 이전 idle 콜백을 무효화한다. */
    private void scheduleIdle(NunchiGame game, String code, NunchiSession session) {
        session.idle = cancel(session.idle);
        final long gen = ++session.idleGen;
        session.idleDeadlineEpochMs = System.currentTimeMillis() + timing.idleTimeout().toMillis();
        session.idle = schedule(() -> onIdleTimeout(game, code, gen), timing.idleTimeout());
    }

    /** 윈도우 타이머를 (재)예약한다. generation을 올려 이전 윈도우 콜백을 무효화한다. */
    private void scheduleWindow(NunchiGame game, String code, NunchiSession session) {
        session.window = cancel(session.window);
        final long gen = ++session.windowGen;
        session.window = schedule(() -> onWindowClose(game, code, gen), timing.numberWindow());
    }

    private void cancelWindow(NunchiSession session) {
        session.window = cancel(session.window);
        session.windowGen++; // 진행 중일 수 있는 윈도우 콜백 무효화
    }

    private void cancelIdle(NunchiSession session) {
        session.idle = cancel(session.idle);
        session.idleGen++; // 진행 중일 수 있는 idle 콜백 무효화
    }

    private void cancelAll(NunchiSession session) {
        cancelWindow(session);
        cancelIdle(session);
        session.description = cancel(session.description);
        session.cooldown = cancel(session.cooldown);
        session.hardCap = cancel(session.hardCap);
    }

    private ScheduledFuture<?> schedule(Runnable task, Duration delay) {
        return taskScheduler.schedule(task, Instant.now().plus(delay));
    }

    private ScheduledFuture<?> cancel(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(false);
        }
        return null;
    }

    /** joinCode별 Flow 상태. 모든 필드 접근은 {@code lock} 아래에서만 한다. */
    private static final class NunchiSession {
        private final Object lock = new Object();
        private final List<String> stood = new ArrayList<>();
        private ScheduledFuture<?> description; // 규칙 설명 → PLAYING 전이 타이머(시작 시 한 번)
        private ScheduledFuture<?> window;
        private ScheduledFuture<?> idle;
        private ScheduledFuture<?> cooldown;
        private ScheduledFuture<?> hardCap;
        private ScheduledFuture<?> result; // 종료 후 결과 대기 → 다음 단계 전이 타이머(결정 9, cancelAll 이후 단발 예약)
        private long windowGen;          // 윈도우 타이머 세대 — stale 발화 가드
        private long idleGen;            // idle 타이머 세대 — stale 발화 가드
        private long idleDeadlineEpochMs; // 실제 예약된 idle 발화 절대 시각(브로드캐스트와 일치)
        private long hardCapEpochMs;      // PLAYING 시작 시 고정되는 하드캡 절대 시각(결정 8 — 불변)
        private boolean finished;
    }
}
