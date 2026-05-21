package coffeeshout.room.domain.service;

import coffeeshout.exception.GlobalErrorCode;
import coffeeshout.exception.custom.BusinessException;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomQueryService {

    private final RoomRepository roomRepository;

    public Room getByJoinCode(@NonNull JoinCode joinCode) {
        return roomRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_EXIST, "방이 존재하지 않습니다."));
    }

    public boolean existsByJoinCode(@NonNull JoinCode joinCode) {
        return roomRepository.existsByJoinCode(joinCode);
    }

    public List<Player> getPlayers(@NonNull JoinCode joinCode) {
        final Room room = getByJoinCode(joinCode);
        return List.copyOf(room.getPlayers());
    }

    public boolean existsPlayer(@NonNull JoinCode joinCode, @NonNull PlayerName playerName) {
        return roomRepository.findByJoinCode(joinCode)
                .map(room -> room.getPlayers().stream()
                        .anyMatch(player -> player.sameName(playerName)))
                .orElse(false);
    }
}
