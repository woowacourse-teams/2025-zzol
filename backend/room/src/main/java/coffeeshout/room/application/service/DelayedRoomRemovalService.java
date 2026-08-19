package coffeeshout.room.application.service;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.RoomLifecycleEvent;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.SystemException;
import coffeeshout.global.redis.stream.StreamPublisher;
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
    private final Duration finishedRemoveDuration;
    private final RoomCommandService roomCommandService;
    private final WsRecoveryService wsRecoveryService;
    private final StreamPublisher streamPublisher;

    public DelayedRoomRemovalService(
            @Qualifier("delayRemovalScheduler") TaskScheduler taskScheduler,
            @Value("${room.removalDelay}") Duration removalDelay,
            @Value("${room.finishedRemovalDelay:30s}") Duration finishedRemovalDelay,
            RoomCommandService roomCommandService,
            WsRecoveryService wsRecoveryService,
            StreamPublisher streamPublisher) {
        validateRemovalDuration(removalDelay);
        validateRemovalDuration(finishedRemovalDelay);
        this.taskScheduler = taskScheduler;
        this.removeDuration = removalDelay;
        this.finishedRemoveDuration = finishedRemovalDelay;
        this.roomCommandService = roomCommandService;
        this.wsRecoveryService = wsRecoveryService;
        this.streamPublisher = streamPublisher;
    }

    private void validateRemovalDuration(Duration removalDelay) {
        if (removalDelay == null || removalDelay.isNegative() || removalDelay.isZero()) {
            throw new SystemException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "지연 삭제 시간은 양수여야 합니다.");
        }
    }

    /**
     * 방 최대 수명. 생성 시점에 걸어 두는 백스톱이라 게임이 언제 끝나는지와 무관하다.
     */
    public void scheduleRemoveRoom(JoinCode joinCode) {
        schedule(joinCode, removeDuration);
    }

    /**
     * 룰렛까지 끝나({@code DONE}) 더 쓸 일이 없는 방을 짧은 지연으로 정리한다.
     *
     * <p>게임이 끝나면 클라이언트는 결과 화면으로 넘어가며 소켓을 끊는데, 그 시점 방은 이미 {@code DONE}이라
     * {@code DelayedPlayerRemovalService}의 {@code isReadyState} 가드에 걸려 플레이어 제거가 스케줄되지 않는다.
     * 그래서 방이 비지 않고 참가자를 품은 채 생성 기준 최대 수명까지 남아, 그동안 전원이 "다른 방 참여 중"으로
     * 판정돼 친구 초대를 받지 못했다. 끝난 방은 여기서 스스로 정리한다.
     *
     * <p>0이 아니라 짧은 지연을 두는 것은 룰렛 결과 브로드캐스트를 놓친 클라이언트의 재연결 여지를 남기기 위해서다.
     */
    public void scheduleRemoveFinishedRoom(JoinCode joinCode) {
        schedule(joinCode, finishedRemoveDuration);
    }

    private void schedule(JoinCode joinCode, Duration delay) {
        try {
            log.info("방 지연 삭제 스케줄링: joinCode={}, delay={}초", joinCode.getValue(), delay.getSeconds());

            taskScheduler.schedule(() -> executeRoomRemoval(joinCode), Instant.now().plus(delay));
        } catch (Exception e) {
            log.error("방 제거 스케줄링 실패: joinCode={}", joinCode.getValue(), e);
        }
    }

    private void executeRoomRemoval(JoinCode joinCode) {
        try {
            roomCommandService.delete(joinCode);
            wsRecoveryService.cleanup(joinCode.getValue());
            // 삭제 완료 후 Stream 발행 — GameSession 정리도 생성과 동일한 Stream 경로를 타야
            // 세션을 소유한 인스턴스에 일관되게 도달한다 (ADR-0025 결정 6, in-process 리스너 금지)
            streamPublisher.publish(RoomStreamKey.BROADCAST, new RoomLifecycleEvent.Removed(joinCode.getValue()));
            log.info("방 삭제 완료: joinCode={}", joinCode.getValue());
        } catch (Exception e) {
            log.warn("방 삭제 중 오류 발생: joinCode={}", joinCode.getValue(), e);
        }
    }
}
