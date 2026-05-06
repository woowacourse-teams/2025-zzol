package coffeeshout.friend.infra.persistence;

import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.FriendshipStatus;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.friend.exception.FriendErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.exception.UserErrorCode;
import coffeeshout.user.infra.persistence.UserEntity;
import coffeeshout.user.infra.persistence.UserJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FriendshipRepositoryImpl implements FriendshipRepository {

    private final FriendshipJpaRepository friendshipJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public Friendship save(Friendship friendship) {
        final UserEntity requester = findUserEntityById(friendship.getRequesterId());
        final UserEntity addressee = findUserEntityById(friendship.getAddresseeId());

        if (friendship.getId() != null) {
            final FriendshipEntity entity = friendshipJpaRepository.findById(friendship.getId())
                    .orElseThrow(() -> new BusinessException(FriendErrorCode.FRIEND_REQUEST_NOT_FOUND, "존재하지 않는 친구 요청입니다."));
            if (friendship.isAccepted()) {
                entity.accept();
            }
            return friendshipJpaRepository.save(entity).toDomain();
        }

        final FriendshipEntity entity = new FriendshipEntity(
                requester, addressee, friendship.getStatus(),
                friendship.getCreatedAt(), friendship.getUpdatedAt()
        );
        return friendshipJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Friendship> findById(Long id) {
        return friendshipJpaRepository.findById(id)
                .map(FriendshipEntity::toDomain);
    }

    @Override
    public Optional<Friendship> findBetween(Long userA, Long userB) {
        return friendshipJpaRepository.findBetween(userA, userB)
                .map(FriendshipEntity::toDomain);
    }

    @Override
    public List<Friendship> findReceivedPending(Long userId) {
        return friendshipJpaRepository.findAllByAddressee_IdAndStatus(userId, FriendshipStatus.PENDING)
                .stream()
                .map(FriendshipEntity::toDomain)
                .toList();
    }

    @Override
    public List<Friendship> findSentPending(Long userId) {
        return friendshipJpaRepository.findAllByRequester_IdAndStatus(userId, FriendshipStatus.PENDING)
                .stream()
                .map(FriendshipEntity::toDomain)
                .toList();
    }

    @Override
    public List<Friendship> findAcceptedOf(Long userId) {
        return friendshipJpaRepository.findAllAcceptedOf(userId)
                .stream()
                .map(FriendshipEntity::toDomain)
                .toList();
    }

    @Override
    public void delete(Friendship friendship) {
        friendshipJpaRepository.deleteById(friendship.getId());
    }

    private UserEntity findUserEntityById(Long userId) {
        return userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다. id=" + userId));
    }
}
