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
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

/**
 * 눈치게임 Flow(ADR-0031 — 난이도 최상). 현 {@code FlowScheduler} SPI는 단발·취소불가라 윈도우(300ms)·
 * 쿨다운·idle·하드캡을 동적으로 cancel·reschedule할 수 없다(N2). 따라서 raw {@link TaskScheduler}를
 * 직접 주입하고 joinCode별 {@link NunchiSession}에 6개 타이머 future(설명·ready·윈도우·idle·쿨다운·하드캡)를
 * 들어 미세 제어한다. 세션은 자기 {@link NunchiGame}·{@link JoinCode}도 함께 들어, private 흐름 메서드는
 * {@code session} 하나만 받는다(게임·코드를 매 호출 줄줄이 넘기지 않는다).
 *
 * <p><b>동시성</b>: press는 {@code [nunchi]} 스트림 단일스레드 풀에서, 타이머 콜백은
 * {@code nunchiGameScheduler} 풀에서 도는 <b>서로 다른 스레드</b>다. 컨슈머 단일스레드만으로는 press와
 * 타이머 콜백 사이가 직렬화되지 않으므로, 모든 게임 상태 변경을 joinCode별 {@code session.lock}으로 묶는다.
 * 타이머 콜백은 {@link #withLiveSession}으로 묶어 조회·null·락·finished 가드를 한곳에 모은다.
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
        final NunchiSession session = new NunchiSession(game, joinCode);
        sessions.put(code, session);

        synchronized (session.lock) {
            // 규칙 설명 단계(다른 미니게임과 동일). 이 구간엔 idle·하드캡·윈도우 타이머를 걸지 않고
            // press도 도메인이 거부한다. description이 끝나면 onDescriptionEnd가 READY로 전이한다.
            final long now = System.currentTimeMillis();
            // 알림은 격리한다 — 던지면 아래 description 타이머가 안 걸려 종료 경로 없는 DESCRIPTION에 영구 고착된다.
            notifyQuietly(() -> notifier.notifyDescription(code, now));
            session.description = schedule(() -> onDescriptionEnd(code), timing.description());
        }
    }

    /**
     * 규칙 설명 종료 — READY(곧 시작 카운트다운)로 전이한다. playStartEpochMs(= PLAYING 시작 절대 시각)를
     * 실어 보내, 전 클라이언트가 같은 시각에 동시 진입하도록 한다. ready 타이머가 끝나면 onReadyEnd가 PLAYING으로
     * 전이한다. 이 구간도 입력을 받지 않으므로(도메인 거부) idle·하드캡·윈도우 타이머는 걸지 않는다.
     */
    private void onDescriptionEnd(String code) {
        withLiveSession(code, session -> {
            session.game.startReady();
            final long now = System.currentTimeMillis();
            final long playStartEpochMs = now + timing.ready().toMillis();
            notifyQuietly(() -> notifier.notifyReady(code, now, playStartEpochMs));
            session.ready = schedule(() -> onReadyEnd(code), timing.ready());
        });
    }

    /** 곧 시작 카운트다운 종료 — PLAYING으로 전이하고 idle·하드캡 타이머를 건다(시작 시 한 번, 재예약 없음). */
    private void onReadyEnd(String code) {
        withLiveSession(code, session -> {
            final NunchiGame game = session.game;
            game.startPlaying();
            // 하드캡은 PLAYING 시작 시점부터 잰다(설명·카운트다운 시간은 라운드 상한에 포함하지 않음 — 결정 8 고정 상한).
            final long now = System.currentTimeMillis();
            session.hardCapEpochMs = now + timing.hardCap().toMillis();
            scheduleIdle(session); // idleDeadlineEpochMs 저장 포함 — notify 전에 걸어 종료 경로 보장

            notifyQuietly(() -> notifier.notifyPlaying(code, game.getCurrentNumber(), snapshotStood(session),
                    now, session.idleDeadlineEpochMs, session.hardCapEpochMs));

            session.hardCap = schedule(() -> onHardCap(code), timing.hardCap());
        });
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
            if (session.finished || session.finishing) {
                return; // 종료됐거나 전원 입력으로 종료 대기 중 — 더 받을 입력이 없다
            }
            final PressResult result = game.press(gamer, at);
            switch (result.outcome()) {
                case STOOD -> onStood(session, gamer);
                case COLLIDED -> onCollided(session, result);
                case IGNORED -> log.warn("눈치게임 press 무시(이미 입력/쿨다운 밖/종료): joinCode={}, player={}",
                        code, gamer.getName());
            }
        }
    }

    private void onStood(NunchiSession session, Gamer gamer) {
        session.stood.add(gamer.getName());
        scheduleIdle(session);   // 유효 입력이므로 idle 리셋(N6) — idleDeadlineEpochMs 갱신
        scheduleWindow(session); // 이 번호가 윈도우 안에 또 눌리지 않으면 solo 확정(N2)

        notifyQuietly(() -> notifier.notifyStood(session.code, gamer.getName(), session.game.getCurrentNumber(),
                System.currentTimeMillis(), session.idleDeadlineEpochMs));

        finishIfAllPressed(session);
    }

    private void onCollided(NunchiSession session, PressResult result) {
        final List<String> collided = result.collidedGroup().stream().map(Gamer::getName).toList();
        session.stood.removeAll(collided); // 충돌자는 깨끗이 선 사람이 아니므로 stood 스냅샷에서 제거

        cancelWindow(session); // 충돌 확정 — 이 번호의 윈도우는 끝
        cancelIdle(session);   // 쿨다운 동안 idle 일시정지(N6, 쿨다운을 idle로 오인 방지)

        final long now = System.currentTimeMillis();
        final long resumeAt = now + timing.collisionCooldown().toMillis();
        notifyQuietly(() -> notifier.notifyCollisionCooldown(session.code, result.number(), collided, now, resumeAt));

        session.cooldown = cancel(session.cooldown);
        session.cooldown = schedule(() -> onCooldownEnd(session.code), timing.collisionCooldown());

        // 2명이 서로 충돌해 남는 사람이 없으면 즉시 종료(N3 — 무한 쿨다운 방지)
        finishIfAllPressed(session);
    }

    private void onWindowClose(String code, long gen) {
        withLiveSession(code, session -> {
            if (gen != session.windowGen) {
                return; // 대체된 stale 윈도우 — 무시
            }
            session.game.closeWindow(); // pending이 살아있으면 solo 확정, 카운터 전진
            broadcastPlaying(session);
            finishIfAllPressed(session);
        });
    }

    private void onCooldownEnd(String code) {
        withLiveSession(code, session -> {
            session.game.endCooldown(); // 같은 번호로 PLAYING 재개
            scheduleIdle(session); // idle 재개(N6) — idleDeadlineEpochMs 갱신
            broadcastPlaying(session);
        });
    }

    /** idle 타임아웃 — 캡처한 generation이 최신일 때만 종료(대체된 stale idle 무시). */
    private void onIdleTimeout(String code, long gen) {
        withLiveSession(code, session -> {
            if (gen != session.idleGen) {
                return; // 대체된 stale idle — 무시
            }
            log.info("눈치게임 idle 타임아웃 종료: joinCode={}", code);
            finish(session);
        });
    }

    /** 하드캡 — 시작 시 한 번만 예약되고 재예약되지 않으므로 generation 불필요. finished만 확인한다. */
    private void onHardCap(String code) {
        withLiveSession(code, session -> {
            log.info("눈치게임 하드캡 종료: joinCode={}", code);
            finish(session);
        });
    }

    /**
     * 전원이 입력을 마쳤으면 조기 종료를 예약한다(결정 5). 곧장 DONE으로 넘기지 않고 마지막 입력을 잠깐
     * 보여준 뒤({@code allPressedDelay}) 종료한다 — 마지막 사람이 서는 순간 화면이 즉시 넘어가지 않도록.
     * idle·하드캡 타임아웃은 이미 "시간이 다 된" 종료라 이 경로를 타지 않고 곧바로 {@link #finish}한다.
     * 호출자는 락을 보유한 상태여야 한다.
     */
    private void finishIfAllPressed(NunchiSession session) {
        if (session.game.isAllPressed()) {
            scheduleFinish(session);
        }
    }

    /**
     * 전원 입력 조기 종료를 {@code allPressedDelay} 후로 미룬다. 더 받을 입력이 없으므로 입력·재개 타이머
     * (window·idle·cooldown)는 멈추고 단발 finish 타이머만 남긴다. 하드캡은 안전망으로 남겨, 짧은 delay
     * 동안 만에 하나 먼저 발화하더라도 {@code finished} 가드로 멱등 처리된다. 호출자는 락을 보유한 상태여야 한다.
     */
    private void scheduleFinish(NunchiSession session) {
        if (session.finishing || session.finished) {
            return; // 이미 종료 예약/완료 — 중복 예약 방지
        }
        session.finishing = true;
        cancelWindow(session);
        cancelIdle(session);
        session.cooldown = cancel(session.cooldown);
        log.info("눈치게임 전원 입력 — {}ms 후 종료: joinCode={}", timing.allPressedDelay().toMillis(), session.code);
        session.finish = schedule(() -> onFinishDelayEnd(session.code), timing.allPressedDelay());
    }

    /** 전원 입력 후 {@code allPressedDelay} 경과 — 이제 실제 종료(DONE). 하드캡이 먼저 종료했으면 finished 가드로 무시. */
    private void onFinishDelayEnd(String code) {
        withLiveSession(code, this::finish);
    }

    /**
     * 멱등 종료. 호출자는 락을 보유한 상태여야 한다. DONE을 알린 뒤 곧바로 라운드를 확정하고 다음 단계로 넘긴다.
     */
    private void finish(NunchiSession session) {
        if (session.finished) {
            return;
        }
        session.finished = true;
        cancelAll(session);

        session.game.finishByTimeout(); // pending solo 확정 + 미입력자 MISS + DONE
        notifyQuietly(() -> notifier.notifyDone(session.code));

        finalizeGame(session);
    }

    /**
     * 라운드를 확정하고 다음 단계(SCORE_BOARD 전이)로 넘긴다. 호출자({@link #finish})는 락을 보유한 상태여야 한다.
     *
     * <p>순서 불변식(ADR-0025 결정 5): {@code finishGame()}으로 roundCount 확정·상태 복귀 후
     * {@link MiniGameFinishedEvent}를 발행한다(결과 저장·라운드 전진·확률 조정·SCORE_BOARD 전이 유발). 발행은
     * 동기로, 저장 리스너 실패가 흐름을 막지 않도록 한다 — BlockStacking/SpeedTouch 동일.
     */
    private void finalizeGame(NunchiSession session) {
        final int roundCount = gameSessionService.finishGame(session.joinCode);
        eventPublisher.publishEvent(new MiniGameFinishedEvent(
                session.code, MiniGameType.NUNCHI_GAME.name(), session.game.getResult().toRankMap(), roundCount));
        sessions.remove(session.code);
    }

    private void broadcastPlaying(NunchiSession session) {
        notifyQuietly(() -> notifier.notifyPlaying(session.code, session.game.getCurrentNumber(),
                snapshotStood(session), System.currentTimeMillis(),
                session.idleDeadlineEpochMs, session.hardCapEpochMs));
    }

    /**
     * 타이머 콜백 공통 가드: 세션 조회 → null(이미 정리됨)·finished(이미 종료) 시 무시 → 락 아래에서 실행.
     * {@code code}-keyed 조회는 finalize 후 제거된 세션을 감지하는 역할도 하므로 람다에 세션 참조를 직접
     * 캡처하지 않고 매번 조회한다. 락 안에서 도므로 window/idle은 stale generation 체크를 첫 줄에 더한다.
     */
    private void withLiveSession(String code, Consumer<NunchiSession> action) {
        final NunchiSession session = sessions.get(code);
        if (session == null) {
            return;
        }
        synchronized (session.lock) {
            if (session.finished) {
                return;
            }
            action.accept(session);
        }
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
    private void scheduleIdle(NunchiSession session) {
        session.idle = cancel(session.idle);
        final long gen = ++session.idleGen;
        session.idleDeadlineEpochMs = System.currentTimeMillis() + timing.idleTimeout().toMillis();
        session.idle = schedule(() -> onIdleTimeout(session.code, gen), timing.idleTimeout());
    }

    /** 윈도우 타이머를 (재)예약한다. generation을 올려 이전 윈도우 콜백을 무효화한다. */
    private void scheduleWindow(NunchiSession session) {
        session.window = cancel(session.window);
        final long gen = ++session.windowGen;
        session.window = schedule(() -> onWindowClose(session.code, gen), timing.numberWindow());
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
        session.ready = cancel(session.ready);
        session.cooldown = cancel(session.cooldown);
        session.hardCap = cancel(session.hardCap);
        session.finish = cancel(session.finish);
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

    /** joinCode별 Flow 상태. 자기 게임·코드를 들고, 모든 필드 접근은 {@code lock} 아래에서만 한다. */
    private static final class NunchiSession {
        private final Object lock = new Object();
        private final NunchiGame game;     // 이 세션의 도메인 상태기계(startFlow 시점 고정)
        private final JoinCode joinCode;   // finalize 시 finishGame에 그대로 전달
        private final String code;         // joinCode.getValue() 캐시 — 맵 키·알림 인자로 빈번히 쓰임
        private final List<String> stood = new ArrayList<>();
        private ScheduledFuture<?> description; // 규칙 설명 → READY 전이 타이머(시작 시 한 번)
        private ScheduledFuture<?> ready;       // 곧 시작 카운트다운 → PLAYING 전이 타이머(시작 시 한 번)
        private ScheduledFuture<?> window;
        private ScheduledFuture<?> idle;
        private ScheduledFuture<?> cooldown;
        private ScheduledFuture<?> hardCap;
        private ScheduledFuture<?> finish;  // 전원 입력 → allPressedDelay 후 DONE 전이 타이머(결정 5, 단발 예약)
        private long windowGen;          // 윈도우 타이머 세대 — stale 발화 가드
        private long idleGen;            // idle 타이머 세대 — stale 발화 가드
        private long idleDeadlineEpochMs; // 실제 예약된 idle 발화 절대 시각(브로드캐스트와 일치)
        private long hardCapEpochMs;      // PLAYING 시작 시 고정되는 하드캡 절대 시각(결정 8 — 불변)
        private boolean finishing;        // 전원 입력으로 종료 대기 중(allPressedDelay) — 추가 입력·재예약 차단
        private boolean finished;

        private NunchiSession(NunchiGame game, JoinCode joinCode) {
            this.game = game;
            this.joinCode = joinCode;
            this.code = joinCode.getValue();
        }
    }
}
