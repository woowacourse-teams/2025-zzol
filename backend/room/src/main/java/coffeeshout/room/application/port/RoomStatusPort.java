package coffeeshout.room.application.port;

import coffeeshout.room.domain.RoomState;
import coffeeshout.room.infra.persistence.RoomEntity;

public interface RoomStatusPort {

    void updateStatus(String joinCode, RoomState state);

    void updateStatus(RoomEntity roomEntity, RoomState state);
}
