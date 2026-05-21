package coffeeshout.room.application.port;

import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.RoomEntity;
import java.util.List;
import java.util.Optional;

public interface PlayerEntityRepository {

    PlayerEntity save(PlayerEntity playerEntity);

    Optional<PlayerEntity> findByRoomSessionAndPlayerName(RoomEntity roomSession, String playerName);

    List<PlayerEntity> findByRoomSessionAndPlayerNameIn(RoomEntity roomSession, List<String> playerNames);

    List<PlayerEntity> findAllByPlayerName(String playerName);

    List<PlayerEntity> findAllByRoomSession(RoomEntity roomSession);

    List<PlayerEntity> findAllByRoomSessionIn(List<RoomEntity> roomSessions);
}
