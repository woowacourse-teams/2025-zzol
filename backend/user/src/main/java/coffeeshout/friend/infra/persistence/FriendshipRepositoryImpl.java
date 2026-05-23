package coffeeshout.friend.infra.persistence;

import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.FriendshipStatus;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.friend.domain.FriendErrorCode;
import coffeeshout.global.exception.custom.BusinessException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FriendshipRepositoryImpl implements FriendshipRepository {

    private final FriendshipJpaRepository friendshipJpaRepository;

    @Override
    public Friendship save(Friendship friendship) {
        if (friendship.getId() != null) {
            final FriendshipEntity entity = friendshipJpaRepository.findById(friendship.getId())
                    .orElseThrow(() -> new BusinessException(FriendErrorCode.FRIEND_REQUEST_NOT_FOUND, "존재하지 않는 친구 요청입니다."));
            if (friendship.isAccepted()) {
                entity.accept();
            }
            return friendshipJpaRepository.save(entity).toDomain();
        }

        final FriendshipEntity entity = new FriendshipEntity(
                friendship.getRequesterId(), friendship.getAddresseeId(),
                friendship.getStatus(), friendship.getCreatedAt(), friendship.getUpdatedAt()
        );
        return friendshipJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Friendship> findById(Long id) {
        return friendshipJpaRepository.findById(id).map(FriendshipEntity::toDomain);
    }

    @Override
    public Optional<Friendship> findBetween(Long userA, Long userB) {
        return friendshipJpaRepository.findBetween(userA, userB).map(FriendshipEntity::toDomain);
    }

    @Override
    public List<Friendship> findReceivedPending(Long userId) {
        return friendshipJpaRepository.findAllByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING)
                .stream().map(FriendshipEntity::toDomain).toList();
    }

    @Override
    public List<Friendship> findSentPending(Long userId) {
        return friendshipJpaRepository.findAllByRequesterIdAndStatus(userId, FriendshipStatus.PENDING)
                .stream().map(FriendshipEntity::toDomain).toList();
    }

    @Override
    public List<Friendship> findAcceptedOf(Long userId) {
        return friendshipJpaRepository.findAllAcceptedOf(userId, FriendshipStatus.ACCEPTED)
                .stream().map(FriendshipEntity::toDomain).toList();
    }

    @Override
    public void delete(Friendship friendship) {
        friendshipJpaRepository.deleteById(friendship.getId());
    }

    @Override
    public List<Friendship> findAllBetween(Long myId, List<Long> targetUserIds) {
        if (targetUserIds.isEmpty()) {
            return List.of();
        }
        return friendshipJpaRepository.findAllBetween(myId, targetUserIds)
                .stream().map(FriendshipEntity::toDomain).toList();
    }
}
