package coffeeshout.room.application.service;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.domain.event.RoomRemovedEvent;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.websocket.WsRecoveryService;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DelayedRoomRemovalService {

    private final TaskScheduler taskScheduler;
    private final Duration removeDuration;
    private final RoomCommandService roomCommandService;
    private final WsRecoveryService wsRecoveryService;
    private final StreamPublisher streamPublisher;

    public DelayedRoomRemovalService(
            @Qualifier("delayRemovalScheduler") TaskScheduler taskScheduler,
            @Value("${room.removalDelay}") Duration removalDelay,
            RoomCommandService roomCommandService,
            WsRecoveryService wsRecoveryService,
            StreamPublisher streamPublisher) {
        validateRemovalDuration(removalDelay);
        this.taskScheduler = taskScheduler;
        this.removeDuration = removalDelay;
        this.roomCommandService = roomCommandService;
        this.wsRecoveryService = wsRecoveryService;
        this.streamPublisher = streamPublisher;
    }

    private void validateRemovalDuration(Duration removalDelay) {
        if (removalDelay == null || removalDelay.isNegative() || removalDelay.isZero()) {
            throw new IllegalArgumentException("지연 삭제 시간은 양수여야 합니다.");
        }
    }

    public void scheduleRemoveRoom(JoinCode joinCode) {
        try {
            log.info("방 지연 삭제 스케줄링: joinCode={}, delay={}초",
                    joinCode.getValue(), removeDuration.getSeconds());

            taskScheduler.schedule(() -> executeRoomRemoval(joinCode), Instant.now().plus(removeDuration));
        } catch (Exception e) {
            log.error("방 제거 스케줄링 실패: joinCode={}", joinCode.getValue(), e);
        }
    }

    private void executeRoomRemoval(JoinCode joinCode) {
        try {
            roomCommandService.delete(joinCode);
            wsRecoveryService.cleanup(joinCode.getValue());
            // 삭제 완료 후 Stream 발행 — GameSession 정리도 생성과 동일한 Stream 경로를 타야
            // 세션을 소유한 인스턴스에 일관되게 도달한다 (ADR-0023 결정 6, in-process 리스너 금지)
            streamPublisher.publish(RoomStreamKey.BROADCAST, new RoomRemovedEvent(joinCode.getValue()));
            log.info("방 삭제 완료: joinCode={}", joinCode.getValue());
        } catch (Exception e) {
            log.warn("방 삭제 중 오류 발생: joinCode={}", joinCode.getValue(), e);
        }
    }
}
