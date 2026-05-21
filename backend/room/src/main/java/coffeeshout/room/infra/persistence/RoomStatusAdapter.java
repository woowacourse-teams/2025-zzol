package coffeeshout.room.infra.persistence;

import coffeeshout.room.application.port.RoomStatusPort;
import coffeeshout.room.domain.RoomState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomStatusAdapter implements RoomStatusPort {

    private final RoomJpaRepository roomJpaRepository;

    @Override
    public void updateStatus(String joinCode, RoomState state) {
        roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc(joinCode)
                .ifPresent(entity -> entity.updateRoomStatus(state));
    }
}
