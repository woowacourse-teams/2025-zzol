package coffeeshout.room.application.service;

import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.friend.domain.event.RoomInvitationSentEvent;
import coffeeshout.friend.exception.FriendErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomInvitationService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final FriendshipRepository friendshipRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public void invite(Long inviterUserId, Long targetUserId, String rawJoinCode) {
        final User inviter = userRepository.findById(inviterUserId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."));
        validateUserExists(targetUserId);

        final Room room = roomRepository.findByJoinCode(new JoinCode(rawJoinCode))
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND, "존재하지 않는 방입니다."));

        validateRoomIsLobby(room);
        validateInviterInRoom(room, inviterUserId);
        validateFriendship(inviterUserId, targetUserId);

        eventPublisher.publishEvent(new RoomInvitationSentEvent(
                inviterUserId, inviter.getNickname().value(), targetUserId, rawJoinCode
        ));
    }

    private void validateUserExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."));
    }

    private void validateRoomIsLobby(Room room) {
        if (!room.isReadyState()) {
            throw new BusinessException(RoomErrorCode.ROOM_NOT_READY_TO_JOIN, "방이 로비 상태일 때만 초대할 수 있습니다.");
        }
    }

    private void validateInviterInRoom(Room room, Long inviterUserId) {
        final boolean inRoom = room.getPlayers().stream()
                .anyMatch(player -> inviterUserId.equals(player.getUserId()));
        if (!inRoom) {
            throw new BusinessException(RoomErrorCode.INVITER_NOT_IN_ROOM, "방에 참여 중인 사용자만 초대할 수 있습니다.");
        }
    }

    private void validateFriendship(Long inviterUserId, Long targetUserId) {
        friendshipRepository.findBetween(inviterUserId, targetUserId)
                .filter(Friendship::isAccepted)
                .orElseThrow(() -> new BusinessException(FriendErrorCode.NOT_FRIEND, "친구 관계가 아닌 사용자입니다."));
    }
}
