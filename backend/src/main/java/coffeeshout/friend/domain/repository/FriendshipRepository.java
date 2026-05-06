package coffeeshout.friend.domain.repository;

import coffeeshout.friend.domain.Friendship;
import java.util.List;
import java.util.Optional;

public interface FriendshipRepository {

    Friendship save(Friendship friendship);

    Optional<Friendship> findById(Long id);

    Optional<Friendship> findBetween(Long userA, Long userB);

    List<Friendship> findReceivedPending(Long userId);

    List<Friendship> findSentPending(Long userId);

    List<Friendship> findAcceptedOf(Long userId);

    void delete(Friendship friendship);
}
