package coffeeshout.global.websocket.recovery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 게임 복구 관련 이벤트 리스너
 * 방 종료 시 Recovery Stream을 정리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameRecoveryEventListener {

    private final GameRecoveryService gameRecoveryService;

    /**
     * 방 종료 시 복구 Stream 정리
     *
     * TODO: RoomClosedEvent 또는 유사 이벤트가 추가되면 연결
     *
     * @EventListener
     * public void onRoomClosed(RoomClosedEvent event) {
     *     String joinCode = event.getJoinCode();
     *     log.info("방 종료로 복구 Stream 정리: joinCode={}", joinCode);
     *     gameRecoveryService.cleanup(joinCode);
     * }
     */

    /**
     * 수동으로 특정 방의 복구 데이터 정리
     * (관리자 API 등에서 호출 가능)
     *
     * @param joinCode 방 코드
     */
    public void cleanupRoom(String joinCode) {
        log.info("수동 복구 Stream 정리: joinCode={}", joinCode);
        gameRecoveryService.cleanup(joinCode);
    }
}
