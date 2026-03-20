package coffeeshout.global.websocket.lifecycle;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

/**
 * WebSocket Graceful Shutdown 핸들러
 * <p>
 * Spring Boot 애플리케이션 종료 시 활성 WebSocket 연결이 모두 종료될 때까지 대기합니다.
 * 설정된 시간까지 대기하며, 모든 연결이 종료되면 즉시 shutdown을 완료합니다.
 * </p>
 */
@Slf4j
@Component
public class WebSocketGracefulShutdownHandler implements SmartLifecycle {

    private static final Duration STATUS_CHECK_INTERVAL = Duration.ofSeconds(5);

    private final WebSocketMessageBrokerStats webSocketMessageBrokerStats;
    private final TaskScheduler taskScheduler;
    private final Duration shutdownWaitDuration;

    private volatile boolean isRunning = false;
    @Getter
    private volatile boolean isShuttingDown = false;
    private final AtomicReference<CompletableFuture<Void>> shutdownFuture = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> statusCheckTask = new AtomicReference<>();

    public WebSocketGracefulShutdownHandler(
            WebSocketMessageBrokerStats webSocketMessageBrokerStats,
            @Qualifier("delayRemovalScheduler") TaskScheduler taskScheduler,
            @Value("${spring.lifecycle.timeout-per-shutdown-phase}") Duration shutdownWaitDuration) {
        this.webSocketMessageBrokerStats = webSocketMessageBrokerStats;
        this.taskScheduler = taskScheduler;
        this.shutdownWaitDuration = shutdownWaitDuration;
    }

    @Override
    public void start() {
        isRunning = true;
        log.info("▶️ WebSocketGracefulShutdownHandler 시작됨");
    }

    @Override
    public void stop() {
        stop(() -> {});
    }

    @Override
    public void stop(@NonNull Runnable callback) {
        if (isShuttingDown) {
            log.warn("⚠️ 이미 WebSocket Graceful Shutdown이 진행 중입니다");
            callback.run();
            return;
        }

        log.info("🛑 WebSocket Graceful Shutdown 시작");

        final int currentConnections = getWebSocketSessionCount();

        // 활성 연결이 없으면 즉시 종료
        if (currentConnections == 0) {
            log.info("✅ 활성 WebSocket 연결 없음. 즉시 종료");
            isRunning = false;
            callback.run();
            return;
        }

        // Shutdown 모드 활성화
        isShuttingDown = true;
        shutdownFuture.set(new CompletableFuture<>());

        final long timeoutSeconds = shutdownWaitDuration.toSeconds();
        final long displayMinutes = timeoutSeconds / 60;
        final long displaySeconds = timeoutSeconds % 60;
        log.info("⏳ {} 개의 활성 WebSocket 연결 종료 대기 중... (최대 {}분 {}초)", currentConnections, displayMinutes, displaySeconds);

        // 주기적인 상태 로깅 스케줄링
        scheduleStatusLogging();

        // 타임아웃과 함께 대기 (이벤트 기반 - CompletableFuture 사용)
        try {
            final CompletableFuture<Void> future = shutdownFuture.get();
            if (future != null) {
                future.get(shutdownWaitDuration.toMillis(), TimeUnit.MILLISECONDS);
            }
            log.info("✅ 모든 WebSocket 연결 정상 종료 완료");
        } catch (TimeoutException e) {
            final int remaining = getWebSocketSessionCount();
            log.warn("⚠️ Graceful Shutdown 타임아웃 ({}분 {}초): 활성 연결 {} 개가 남아있습니다. 강제 종료합니다.",
                    displayMinutes, displaySeconds, remaining);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Graceful Shutdown 중단됨", e);
        } catch (Exception e) {
            log.error("❌ Graceful Shutdown 중 예외 발생", e);
        } finally {
            cleanup();
            callback.run();
        }
    }

    /**
     * Graceful Shutdown 진행 상황을 주기적으로 체크
     * <p>
     * STATUS_CHECK_INTERVAL마다 남은 연결 수를 확인하고,
     * 모든 세션이 종료되면 Shutdown을 완료합니다.
     * </p>
     */
    private void scheduleStatusLogging() {
        statusCheckTask.set(taskScheduler.scheduleAtFixedRate(() -> {
            try {
                final CompletableFuture<Void> future = shutdownFuture.get();
                if (future == null || future.isDone()) {
                    return;
                }

                final int remaining = getWebSocketSessionCount();
                log.info("📊 Graceful Shutdown 진행 중: 남은 연결 {} 개", remaining);

                // 모든 세션이 종료되면 Shutdown 완료
                if (remaining == 0) {
                    log.info("✅ 모든 WebSocket 세션 종료됨. Graceful Shutdown 완료");
                    future.complete(null);
                }
            } catch (Exception e) {
                log.error("❌ Graceful Shutdown 상태 체크 중 오류", e);
            }
        }, STATUS_CHECK_INTERVAL));
    }

    /**
     * Graceful Shutdown 정리 작업
     */
    private void cleanup() {
        cancelStatusCheckTask();
        isShuttingDown = false;
        isRunning = false;
        shutdownFuture.set(null);
    }

    /**
     * 상태 체크 작업 취소
     */
    private void cancelStatusCheckTask() {
        final ScheduledFuture<?> task = statusCheckTask.get();
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            statusCheckTask.set(null);
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        // SmartLifecycle의 phase 값
        // 값이 클수록 나중에 종료됨 (WebSocket은 가장 마지막에 종료되어야 함)
        return Integer.MAX_VALUE;
    }

    private int getWebSocketSessionCount() {
        return Objects.requireNonNull(webSocketMessageBrokerStats.getWebSocketSessionStats(),
                "WebSocketSessionStats를 가져올 수 없습니다.").getWebSocketSessions();
    }
}
