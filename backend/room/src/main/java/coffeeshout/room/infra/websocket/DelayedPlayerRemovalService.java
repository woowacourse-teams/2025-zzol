package coffeeshout.room.infra.websocket;

import coffeeshout.global.exception.custom.BusinessException;
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
            RoomService roomService) {
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
        if (!isRemovalSchedulable(joinCode)) {
            // 지연 삭제를 걸지 않는 경로라 재접속 유예도 없다. 매핑을 남기면 영영 안 지워진다
            stompSessionManager.removeSession(sessionId);
            return;
        }

        log.info(
                "플레이어 지연 삭제 스케줄링: playerKey={}, sessionId={}, delay={}초",
                playerKey,
                sessionId,
                removalDelay.getSeconds());

        playerDisconnectionService.cancelReady(playerKey);

        final ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    executePlayerRemoval(playerKey, sessionId, reason);
                    stompSessionManager.removeSession(sessionId);
                },
                Instant.now().plus(removalDelay));

        scheduledTasks.put(playerKey, future);
    }

    /**
     * 방이 이미 삭제됐으면 지연 삭제를 걸 대상도 없다. 게임이 끝난 방은 짧은 지연 뒤 삭제되므로,
     * 결과 화면에 머물던 클라이언트가 그 뒤 끊으면 방 조회가 예외로 끝난다. 그 예외를 밖으로 내보내면
     * 세션 매핑 정리까지 건너뛰게 된다.
     */
    private boolean isRemovalSchedulable(String joinCode) {
        try {
            return roomService.isReadyState(joinCode);
        } catch (BusinessException e) {
            log.debug("방이 없어 지연 삭제를 건너뛴다: joinCode={}", joinCode);
            return false;
        }
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
            log.info("플레이어 지연 삭제 실행: playerKey={}, sessionId={}, reason={}", playerKey, sessionId, reason);

            playerDisconnectionService.handlePlayerDisconnection(playerKey, sessionId, reason);
            scheduledTasks.remove(playerKey);

        } catch (Exception e) {
            log.error("플레이어 지연 삭제 실행 중 오류 발생: playerKey={}, error={}", playerKey, e.getMessage(), e);
        }
    }
}
