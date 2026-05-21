package coffeeshout.room.infra.websocket;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.application.service.RoomCommandService;
import coffeeshout.room.infra.websocket.event.RoomStateUpdateEvent;
import coffeeshout.websocket.PlayerKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 플레이어 연결 해제 관련 로직을 담당하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerDisconnectionService {

    private final RoomCommandService roomCommandService;
    private final ApplicationEventPublisher eventPublisher;

    public void cancelReady(String playerKeyStr) {
        final PlayerKey playerKey = PlayerKey.parse(playerKeyStr);

        roomCommandService.readyPlayer(new JoinCode(playerKey.joinCode()), new PlayerName(playerKey.playerName()), false);

        eventPublisher.publishEvent(new RoomStateUpdateEvent(playerKey.joinCode(), "PLAYER_SET_READY_FALSE"));
        log.info("삭제 대기된 플레이어 ready 상태 변경 완료: joinCode={}, playerName={}", playerKey.joinCode(), playerKey.playerName());
    }

    /**
     * 플레이어 연결 해제 처리
     */
    public void handlePlayerDisconnection(String playerKeyStr, String sessionId, String reason) {
        try {
            if (!PlayerKey.isValid(playerKeyStr)) {
                log.warn("잘못된 플레이어 키 형식: {}", playerKeyStr);
                return;
            }

            final PlayerKey playerKey = PlayerKey.parse(playerKeyStr);

            log.info("플레이어 연결 해제 처리: joinCode={}, playerName={}, reason={}", playerKey.joinCode(), playerKey.playerName(), reason);

            removePlayerFromRoom(playerKey.joinCode(), playerKey.playerName());

        } catch (Exception e) {
            log.error("플레이어 연결 해제 처리 실패: playerKey={}, sessionId={}, reason={}", playerKeyStr, sessionId, reason, e);
        }
    }

    private void removePlayerFromRoom(String joinCode, String playerName) {
        try {
            final boolean removed = roomCommandService.removePlayer(new JoinCode(joinCode), new PlayerName(playerName));

            if (removed) {
                eventPublisher.publishEvent(new RoomStateUpdateEvent(joinCode, "PLAYER_REMOVED"));
                log.info("플레이어 방에서 제거 완료: joinCode={}, playerName={}", joinCode, playerName);
                return;
            }

            log.warn("플레이어 제거 실패 (이미 없음): joinCode={}, playerName={}", joinCode, playerName);
        } catch (Exception e) {
            log.error("방에서 플레이어 제거 실패: joinCode={}, playerName={}", joinCode, playerName, e);
        }
    }
}
