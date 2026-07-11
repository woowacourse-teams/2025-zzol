package coffeeshout.room.infra.persistence;

import coffeeshout.room.application.port.RoomStatusPort;
import coffeeshout.room.domain.RoomState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RoomStatusAdapter implements RoomStatusPort {

    private final RoomJpaRepository roomJpaRepository;

    // 스트림 컨슈머 스레드(비트랜잭션 체인, RoomGameStartListener)에서 호출되므로 @Transactional로
    // 영속성 컨텍스트를 연다. 없으면 findFirst가 반환한 엔티티가 detached 상태라 updateRoomStatus의
    // 변경이 flush되지 않아 room_status 전이가 유실된다(ADR-0034).
    @Override
    @Transactional
    public void updateStatus(String joinCode, RoomState state) {
        roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc(joinCode)
                .ifPresent(entity -> entity.updateRoomStatus(state));
    }
}
