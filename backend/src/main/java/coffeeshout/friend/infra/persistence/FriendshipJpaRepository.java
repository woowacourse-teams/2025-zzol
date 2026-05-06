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
            WHERE (f.requester.id = :userA AND f.addressee.id = :userB)
               OR (f.requester.id = :userB AND f.addressee.id = :userA)
            """)
    Optional<FriendshipEntity> findBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    List<FriendshipEntity> findAllByAddressee_IdAndStatus(Long addresseeId, FriendshipStatus status);

    List<FriendshipEntity> findAllByRequester_IdAndStatus(Long requesterId, FriendshipStatus status);

    @Query("""
            SELECT f FROM FriendshipEntity f
            WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
              AND f.status = 'ACCEPTED'
            """)
    List<FriendshipEntity> findAllAcceptedOf(@Param("userId") Long userId);
}
