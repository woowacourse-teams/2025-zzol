package coffeeshout.room.application;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.service.RoomCommandService;
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

    public DelayedRoomRemovalService(
            @Qualifier("delayRemovalScheduler") TaskScheduler taskScheduler,
            @Value("${room.removalDelay}") Duration removalDelay,
            RoomCommandService roomCommandService) {
        validateRemovalDuration(removalDelay);
        this.taskScheduler = taskScheduler;
        this.removeDuration = removalDelay;
        this.roomCommandService = roomCommandService;
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
            log.info("방 삭제 완료: joinCode={}", joinCode.getValue());
        } catch (Exception e) {
            log.error("방 삭제 중 오류 발생: joinCode={}", joinCode.getValue(), e);
        }
    }
}
