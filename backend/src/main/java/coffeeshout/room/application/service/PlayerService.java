package coffeeshout.room.application.service;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.domain.JoinCode;
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

    public boolean kickPlayer(String joinCode, String playerName) {
        log.info("JoinCode[{}] 플레이어 강퇴 명령 처리 - 플레이어: {}", joinCode, playerName);

        boolean exists = roomCommandService.removePlayer(new JoinCode(joinCode), new PlayerName(playerName));

        if (exists) {
            final PlayerKickEvent event = new PlayerKickEvent(joinCode, playerName);
            streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
        }

        return exists;
    }

    public List<Player> getPlayers(String joinCode) {
        return roomQueryService.getPlayers(new JoinCode(joinCode));
    }
}
