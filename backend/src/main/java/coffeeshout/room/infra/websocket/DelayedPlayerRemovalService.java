package coffeeshout.room.infra.websocket;

import coffeeshout.room.application.service.RoomService;
import coffeeshout.websocket.StompSessionManager;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DelayedPlayerRemovalService {

    private final TaskScheduler taskScheduler;
    private final Duration removalDelay;
    private final PlayerDisconnectionService playerDisconnectionService;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks;
    private final RoomService roomService;
    private final StompSessionManager stompSessionManager;

    public DelayedPlayerRemovalService(
            @Qualifier("delayRemovalScheduler") TaskScheduler taskScheduler,
            @Value("${player.removalDelay}") Duration removalDelay,
            PlayerDisconnectionService playerDisconnectionService,
            StompSessionManager stompSessionManager,
            RoomService roomService
    ) {
        validateRemovalDuration(removalDelay);
        this.taskScheduler = taskScheduler;
        this.removalDelay = removalDelay;
        this.playerDisconnectionService = playerDisconnectionService;
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.roomService = roomService;
        this.stompSessionManager = stompSessionManager;
    }

    private void validateRemovalDuration(Duration removalDelay) {
        if (removalDelay == null || removalDelay.isNegative() || removalDelay.isZero()) {
            throw new IllegalArgumentException("지연 삭제 시간은 양수여야 합니다.");
        }
    }

    public void schedulePlayerRemoval(String playerKey, String sessionId, String reason) {
        final String joinCode = playerKey.split(":")[0];
        if (!roomService.isReadyState(joinCode)) {
            return;
        }

        log.info("플레이어 지연 삭제 스케줄링: playerKey={}, sessionId={}, delay={}초",
                playerKey, sessionId, removalDelay.getSeconds());

        playerDisconnectionService.cancelReady(playerKey);

        final ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    executePlayerRemoval(playerKey, sessionId, reason);
                    stompSessionManager.removeSession(sessionId);
                },
                Instant.now().plus(removalDelay)
        );

        scheduledTasks.put(playerKey, future);
    }

    public void cancelScheduledRemoval(String playerKey) {
        final ScheduledFuture<?> future = scheduledTasks.remove(playerKey);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.info("플레이어 지연 삭제 취소됨: playerKey={}", playerKey);
        }
    }

    private void executePlayerRemoval(String playerKey, String sessionId, String reason) {
        try {
            log.info("플레이어 지연 삭제 실행: playerKey={}, sessionId={}, reason={}",
                    playerKey, sessionId, reason);

            playerDisconnectionService.handlePlayerDisconnection(playerKey, sessionId, reason);
            scheduledTasks.remove(playerKey);

        } catch (Exception e) {
            log.error("플레이어 지연 삭제 실행 중 오류 발생: playerKey={}, error={}",
                    playerKey, e.getMessage(), e);
        }
    }
}
