package coffeeshout.room.application.service;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.redis.stream.StreamPublisher;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.event.PlayerKickEvent;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.domain.service.RoomQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final RoomQueryService roomQueryService;
    private final RoomCommandService roomCommandService;
    private final ApplicationEventPublisher eventPublisher;
    private final StreamPublisher streamPublisher;

    public boolean checkAndKickPlayer(String joinCode, String callerPlayerName, String targetPlayerName) {
        log.info("JoinCode[{}] 플레이어 강퇴 명령 처리 - 요청자: {}, 대상: {}", joinCode, callerPlayerName, targetPlayerName);

        final JoinCode code = new JoinCode(joinCode);
        final Room room = roomQueryService.getByJoinCode(code);

        if (!room.isHostByName(new PlayerName(callerPlayerName))) {
            throw new BusinessException(RoomErrorCode.NOT_HOST, "호스트만 강퇴할 수 있습니다.");
        }

        final PlayerName target = new PlayerName(targetPlayerName);
        final boolean exists = room.getPlayers().stream()
                .anyMatch(player -> player.sameName(target));

        if (exists) {
            streamPublisher.publish(RoomStreamKey.BROADCAST, new PlayerKickEvent(joinCode, targetPlayerName));
        }

        return exists;
    }

    public void kickPlayer(PlayerKickEvent event) {
        log.info("JoinCode[{}] 플레이어 강퇴 이벤트 처리 - 플레이어: {}", event.joinCode(), event.playerName());

        roomCommandService.removePlayer(new JoinCode(event.joinCode()), new PlayerName(event.playerName()));

        eventPublisher.publishEvent(new PlayerListUpdateEvent(event.joinCode()));
    }

    public List<Player> getPlayers(String joinCode) {
        return roomQueryService.getPlayers(new JoinCode(joinCode));
    }

    public void updatePlayers(PlayerListUpdateEvent event) {
        log.info("JoinCode[{}] 플레이어 목록 업데이트 이벤트 처리", event.joinCode());

        eventPublisher.publishEvent(event);
    }
}
