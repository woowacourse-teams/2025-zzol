package coffeeshout.friend.application.service;

import coffeeshout.friend.application.PresenceTracker;
import coffeeshout.friend.application.port.RoomInvitationValidator;
import coffeeshout.friend.application.port.RoomMembershipQuery;
import coffeeshout.friend.domain.FriendErrorCode;
import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.event.RoomInvitationSentEvent;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserErrorCode;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomInvitationService {

    private final UserRepository userRepository;
    private final RoomInvitationValidator roomInvitationValidator;
    private final RoomMembershipQuery roomMembershipQuery;
    private final PresenceTracker presenceTracker;
    private final FriendshipRepository friendshipRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public void invite(Long inviterUserId, Long targetUserId, String rawJoinCode) {
        final User inviter = userRepository.findById(inviterUserId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."));
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."));

        roomInvitationValidator.validateRoomIsLobby(rawJoinCode);
        roomInvitationValidator.validateInviterInRoom(rawJoinCode, inviterUserId);
        validateFriendship(inviterUserId, targetUserId);
        validateTargetCanBeInvited(targetUserId);

        eventPublisher.publishEvent(new RoomInvitationSentEvent(
                inviterUserId, inviter.getNickname().value(), targetUserId, rawJoinCode
        ));
    }

    /**
     * 초대장은 WebSocket 개인 큐로만 전달되고 저장되지 않는다. 그래서 대상이 접속 중이 아니거나 이미 다른 방에
     * 있어(클라이언트가 초대를 무시한다) 받을 수 없는 상태면, 보낸 쪽에는 성공으로 보이지만 초대장은 사라진다.
     * 받을 수 없는 상태를 여기서 걸러 그 불일치를 없앤다.
     */
    private void validateTargetCanBeInvited(Long targetUserId) {
        if (!presenceTracker.isOnline(targetUserId)) {
            throw new BusinessException(FriendErrorCode.FRIEND_OFFLINE, "접속 중이 아닌 친구는 초대할 수 없습니다.");
        }
        if (roomMembershipQuery.findByUserIds(List.of(targetUserId)).containsKey(targetUserId)) {
            throw new BusinessException(
                    FriendErrorCode.FRIEND_ALREADY_IN_ROOM, "이미 다른 방에 참여 중인 친구는 초대할 수 없습니다.");
        }
    }

    private void validateFriendship(Long inviterUserId, Long targetUserId) {
        friendshipRepository.findBetween(inviterUserId, targetUserId)
                .filter(Friendship::isAccepted)
                .orElseThrow(() -> new BusinessException(FriendErrorCode.NOT_FRIEND, "친구 관계가 아닌 사용자입니다."));
    }
}
