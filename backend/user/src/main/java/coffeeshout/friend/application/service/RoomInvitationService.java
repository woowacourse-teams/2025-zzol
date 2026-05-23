package coffeeshout.friend.application.service;

import coffeeshout.friend.application.port.RoomInvitationValidator;
import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.friend.domain.event.RoomInvitationSentEvent;
import coffeeshout.friend.domain.FriendErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.domain.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomInvitationService {

    private final UserRepository userRepository;
    private final RoomInvitationValidator roomInvitationValidator;
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

        eventPublisher.publishEvent(new RoomInvitationSentEvent(
                inviterUserId, inviter.getNickname().value(), targetUserId, rawJoinCode
        ));
    }

    private void validateFriendship(Long inviterUserId, Long targetUserId) {
        friendshipRepository.findBetween(inviterUserId, targetUserId)
                .filter(Friendship::isAccepted)
                .orElseThrow(() -> new BusinessException(FriendErrorCode.NOT_FRIEND, "친구 관계가 아닌 사용자입니다."));
    }
}
