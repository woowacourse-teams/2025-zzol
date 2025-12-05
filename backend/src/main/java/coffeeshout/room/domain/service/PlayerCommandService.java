package coffeeshout.room.domain.service;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.PlayerType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerCommandService {

    private final RoomQueryService roomQueryService;
    private final RoomCommandService roomCommandService;

    public List<Player> changePlayerReadyState(String joinCode, String playerName, Boolean isReady) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final Player player = room.findPlayer(new PlayerName(playerName));

        if (player.getPlayerType() == PlayerType.HOST) {
            return room.getPlayers();
        }

        player.updateReadyState(isReady);
        roomCommandService.save(room);
        return room.getPlayers();
    }

    public List<Player> getAllPlayers(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        return room.getPlayers();
    }

    public boolean isGuestNameDuplicated(String joinCode, String guestName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        return room.hasDuplicatePlayerName(new PlayerName(guestName));
    }

    public boolean removePlayer(String joinCode, String playerName) {
        final JoinCode code = new JoinCode(joinCode);
        final Room room = roomQueryService.getByJoinCode(code);

        boolean isRemoved = room.removePlayer(new PlayerName(playerName));
        if (room.isEmpty()) {
            roomCommandService.delete(code);
        }
        return isRemoved;
    }
}
