package coffeeshout.room.application.service;

import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.domain.JoinCode;
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

    public boolean publishKickPlayerEvent(String joinCode, String playerName) {
        log.info("JoinCode[{}] 플레이어 강퇴 명령 처리 - 플레이어: {}", joinCode, playerName);

        final boolean exists = roomQueryService.existsPlayer(new JoinCode(joinCode), new PlayerName(playerName));

        if (exists) {
            final PlayerKickEvent event = new PlayerKickEvent(joinCode, playerName);
            streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
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
