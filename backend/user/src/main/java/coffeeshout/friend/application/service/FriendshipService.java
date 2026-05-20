package coffeeshout.friend.application.service;

import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.friend.domain.event.FriendRemovedEvent;
import coffeeshout.friend.domain.event.FriendRequestAcceptedEvent;
import coffeeshout.friend.domain.event.FriendRequestCreatedEvent;
import coffeeshout.friend.domain.event.FriendRequestRejectedEvent;
import coffeeshout.friend.exception.FriendErrorCode;
import coffeeshout.exception.custom.BusinessException;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.user.exception.UserErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

        final User requester = findUser(requesterId);
        eventPublisher.publishEvent(new FriendRequestCreatedEvent(
                saved.getId(), requesterId,
                requester.getUserCode().value(), requester.getNickname().value(),
                targetUserId
        ));
        return saved;
    }

    @Transactional
    public Friendship accept(Long userId, Long requestId) {
        final Friendship friendship = findPendingById(requestId);
        friendship.acceptBy(userId);
        final Friendship saved = friendshipRepository.save(friendship);

        final User requester = findUser(saved.getRequesterId());
        final User addressee = findUser(saved.getAddresseeId());
        eventPublisher.publishEvent(new FriendRequestAcceptedEvent(
                saved.getId(),
                saved.getRequesterId(), requester.getUserCode().value(), requester.getNickname().value(),
                saved.getAddresseeId(), addressee.getUserCode().value(), addressee.getNickname().value()
        ));
        return saved;
    }

    @Transactional
    public void reject(Long userId, Long requestId) {
        final Friendship friendship = findPendingById(requestId);
        friendship.validateRejectableBy(userId);
        friendshipRepository.delete(friendship);

        final User addressee = findUser(friendship.getAddresseeId());
        eventPublisher.publishEvent(new FriendRequestRejectedEvent(
                friendship.getId(), friendship.getRequesterId(), friendship.getAddresseeId(),
                addressee.getUserCode().value(), addressee.getNickname().value()
        ));
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
        final List<Friendship> friendships = friendshipRepository.findReceivedPending(userId);
        final List<Long> requesterIds = friendships.stream().map(Friendship::getRequesterId).toList();
        final Map<Long, User> userById = userRepository.findAllByIds(requesterIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return friendships.stream()
                .map(f -> new FriendRequestWithUser(f.getId(), userById.get(f.getRequesterId()), f.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestWithUser> findSentPending(Long userId) {
        final List<Friendship> friendships = friendshipRepository.findSentPending(userId);
        final List<Long> addresseeIds = friendships.stream().map(Friendship::getAddresseeId).toList();
        final Map<Long, User> userById = userRepository.findAllByIds(addresseeIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return friendships.stream()
                .map(f -> new FriendRequestWithUser(f.getId(), userById.get(f.getAddresseeId()), f.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> findAcceptedFriendIds(Long userId) {
        return friendshipRepository.findAcceptedOf(userId).stream()
                .map(f -> f.counterpartOf(userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendWithUser> findFriends(Long userId) {
        final List<Friendship> friendships = friendshipRepository.findAcceptedOf(userId);
        final List<Long> counterpartIds = friendships.stream().map(f -> f.counterpartOf(userId)).toList();
        final Map<Long, User> userById = userRepository.findAllByIds(counterpartIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return friendships.stream()
                .map(f -> new FriendWithUser(userById.get(f.counterpartOf(userId)), f.getUpdatedAt()))
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 회원입니다. id=" + userId));
    }

    private void validateUserExists(Long userId) {
        findUser(userId);
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
