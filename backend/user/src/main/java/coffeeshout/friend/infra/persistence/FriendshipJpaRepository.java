package coffeeshout.friend.infra.persistence;

import coffeeshout.friend.domain.FriendshipStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipJpaRepository extends JpaRepository<FriendshipEntity, Long> {

    @Query("""
            SELECT f FROM FriendshipEntity f
            WHERE (f.requesterId = :userA AND f.addresseeId = :userB)
               OR (f.requesterId = :userB AND f.addresseeId = :userA)
            """)
    Optional<FriendshipEntity> findBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    List<FriendshipEntity> findAllByAddresseeIdAndStatus(Long addresseeId, FriendshipStatus status);

    List<FriendshipEntity> findAllByRequesterIdAndStatus(Long requesterId, FriendshipStatus status);

    @Query("""
            SELECT f FROM FriendshipEntity f
            WHERE (f.requesterId = :userId OR f.addresseeId = :userId)
              AND f.status = :status
            """)
    List<FriendshipEntity> findAllAcceptedOf(@Param("userId") Long userId,
                                             @Param("status") FriendshipStatus status);

    @Query("""
            SELECT f FROM FriendshipEntity f
            WHERE (f.requesterId = :myId AND f.addresseeId IN :targetIds)
               OR (f.addresseeId = :myId AND f.requesterId IN :targetIds)
            """)
    List<FriendshipEntity> findAllBetween(@Param("myId") Long myId,
                                          @Param("targetIds") List<Long> targetIds);
}
