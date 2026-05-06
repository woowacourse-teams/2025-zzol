package coffeeshout.friend.application.service;

import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.friend.domain.event.FriendRemovedEvent;
import coffeeshout.friend.domain.event.FriendRequestAcceptedEvent;
import coffeeshout.friend.domain.event.FriendRequestCreatedEvent;
import coffeeshout.friend.domain.event.FriendRequestRejectedEvent;
import coffeeshout.friend.exception.FriendErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Friendship sendRequest(Long requesterId, Long targetUserId) {
        validateUserExists(targetUserId);
        validateNoExistingRelation(requesterId, targetUserId);

        final Friendship friendship = Friendship.request(requesterId, targetUserId);
        final Friendship saved = friendshipRepository.save(friendship);
        eventPublisher.publishEvent(new FriendRequestCreatedEvent(saved.getId(), requesterId, targetUserId));
        return saved;
    }

    @Transactional
    public Friendship accept(Long userId, Long requestId) {
        final Friendship friendship = findPendingById(requestId);
        friendship.acceptBy(userId);
        final Friendship saved = friendshipRepository.save(friendship);
        eventPublisher.publishEvent(new FriendRequestAcceptedEvent(saved.getId(), saved.getRequesterId(), saved.getAddresseeId()));
        return saved;
    }

    @Transactional
    public void reject(Long userId, Long requestId) {
        final Friendship friendship = findPendingById(requestId);
        friendship.validateRejectableBy(userId);
        eventPublisher.publishEvent(new FriendRequestRejectedEvent(friendship.getId(), friendship.getRequesterId(), friendship.getAddresseeId()));
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void unfriend(Long userId, Long friendUserId) {
        final Friendship friendship = friendshipRepository.findBetween(userId, friendUserId)
                .filter(Friendship::isAccepted)
                .orElseThrow(() -> new BusinessException(FriendErrorCode.NOT_FRIEND, "친구 관계가 아닌 사용자입니다."));
        friendshipRepository.delete(friendship);
        eventPublisher.publishEvent(new FriendRemovedEvent(userId, friendUserId));
    }

    @Transactional(readOnly = true)
    public List<FriendRequestWithUser> findReceivedPending(Long userId) {
        return friendshipRepository.findReceivedPending(userId).stream()
                .map(f -> {
                    final var requester = findUser(f.getRequesterId());
                    return new FriendRequestWithUser(f.getId(), requester, f.getCreatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestWithUser> findSentPending(Long userId) {
        return friendshipRepository.findSentPending(userId).stream()
                .map(f -> {
                    final var addressee = findUser(f.getAddresseeId());
                    return new FriendRequestWithUser(f.getId(), addressee, f.getCreatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendWithUser> findFriends(Long userId) {
        return friendshipRepository.findAcceptedOf(userId).stream()
                .map(f -> new FriendWithUser(findUser(f.counterpartOf(userId)), f.getUpdatedAt()))
                .toList();
    }

    private coffeeshout.user.domain.User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."));
    }

    private void validateUserExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다. id=" + userId));
    }

    private void validateNoExistingRelation(Long requesterId, Long targetUserId) {
        friendshipRepository.findBetween(requesterId, targetUserId).ifPresent(existing -> {
            if (existing.isAccepted()) {
                throw new BusinessException(FriendErrorCode.FRIEND_ALREADY_EXISTS, "이미 친구인 사용자입니다.");
            }
            throw new BusinessException(FriendErrorCode.FRIEND_REQUEST_ALREADY_SENT, "이미 친구 요청을 보냈거나 받은 상태입니다.");
        });
    }

    private Friendship findPendingById(Long requestId) {
        return friendshipRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(FriendErrorCode.FRIEND_REQUEST_NOT_FOUND, "존재하지 않는 친구 요청입니다."));
    }
}
