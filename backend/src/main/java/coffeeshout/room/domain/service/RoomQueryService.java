package coffeeshout.room.domain.service;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.exception.custom.NotExistElementException;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.repository.RoomRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomQueryService {

    private final RoomRepository roomRepository;

    public Room getByJoinCode(@NonNull JoinCode joinCode) {
        return roomRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new NotExistElementException(GlobalErrorCode.NOT_EXIST, "방이 존재하지 않습니다."));
    }

    public boolean existsByJoinCode(@NonNull JoinCode joinCode) {
        return roomRepository.existsByJoinCode(joinCode);
    }
}
