package coffeeshout.room.application.service;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.PlayerKickEvent;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.domain.service.RoomQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final RoomQueryService roomQueryService;
    private final RoomCommandService roomCommandService;
    private final StreamPublisher streamPublisher;

    public boolean removePlayer(String joinCode, String playerName) {
        final JoinCode code = new JoinCode(joinCode);
        final Room room = roomQueryService.getByJoinCode(code);

        boolean isRemoved = room.removePlayer(new PlayerName(playerName));
        if (room.isEmpty()) {
            roomCommandService.delete(code);
        }
        return isRemoved;
    }

    public boolean kickPlayer(String joinCode, String playerName) {
        log.info("JoinCode[{}] 플레이어 강퇴 명령 처리 - 플레이어: {}", joinCode, playerName);

        boolean exists = removePlayer(joinCode, playerName);

        if (exists) {
            final PlayerKickEvent event = new PlayerKickEvent(joinCode, playerName);
            streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
        }

        return exists;
    }

    public void changePlayerReadyState(String joinCode, String playerName, Boolean isReady) {
        roomCommandService.readyPlayer(new JoinCode(joinCode), new PlayerName(playerName), isReady);
    }

    public List<Player> getPlayers(String joinCode) {
        return roomQueryService.getPlayers(new JoinCode(joinCode));
    }
}
