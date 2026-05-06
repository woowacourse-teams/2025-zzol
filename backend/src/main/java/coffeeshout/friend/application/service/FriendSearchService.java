package coffeeshout.friend.application.service;

import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserCode;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendSearchService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    @Transactional(readOnly = true)
    public List<UserSearchResult> searchByUserCode(Long myId, String rawUserCode) {
        return userRepository.findByUserCode(new UserCode(rawUserCode))
                .filter(user -> !user.getId().equals(myId))
                .map(user -> toSearchResult(myId, user))
                .map(List::of)
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<UserSearchResult> searchByNickname(Long myId, String rawNickname) {
        return userRepository.findAllByNickname(new UserNickname(rawNickname)).stream()
                .filter(user -> !user.getId().equals(myId))
                .map(user -> toSearchResult(myId, user))
                .toList();
    }

    private UserSearchResult toSearchResult(Long myId, User user) {
        final Optional<Friendship> friendship = friendshipRepository.findBetween(myId, user.getId());
        final RelationStatus status = determineRelationStatus(myId, friendship);
        return new UserSearchResult(user, status);
    }

    private RelationStatus determineRelationStatus(Long myId, Optional<Friendship> friendship) {
        if (friendship.isEmpty()) {
            return RelationStatus.NONE;
        }
        final Friendship f = friendship.get();
        if (f.isAccepted()) {
            return RelationStatus.FRIEND;
        }
        if (f.getRequesterId().equals(myId)) {
            return RelationStatus.PENDING_OUTGOING;
        }
        return RelationStatus.PENDING_INCOMING;
    }
}
