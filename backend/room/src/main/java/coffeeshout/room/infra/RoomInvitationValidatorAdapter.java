package coffeeshout.room.infra;

import coffeeshout.friend.application.port.RoomInvitationValidator;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomInvitationValidatorAdapter implements RoomInvitationValidator {

    private final RoomRepository roomRepository;

    @Override
    public void validateRoomIsLobby(String joinCode) {
        final Room room = getRoom(joinCode);
        if (!room.isReadyState()) {
            throw new BusinessException(RoomErrorCode.ROOM_NOT_READY_TO_JOIN, "방이 로비 상태일 때만 초대할 수 있습니다.");
        }
    }

    @Override
    public void validateInviterInRoom(String joinCode, Long inviterUserId) {
        final Room room = getRoom(joinCode);
        final boolean inRoom = room.getPlayers().stream()
                .anyMatch(player -> inviterUserId.equals(player.getUserId()));
        if (!inRoom) {
            throw new BusinessException(RoomErrorCode.INVITER_NOT_IN_ROOM, "방에 참여 중인 사용자만 초대할 수 있습니다.");
        }
    }

    private Room getRoom(String joinCode) {
        return roomRepository.findByJoinCode(new JoinCode(joinCode))
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND, "존재하지 않는 방입니다."));
    }
}
