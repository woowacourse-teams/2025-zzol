package coffeeshout.friend.application.service;

import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.domain.repository.FriendshipRepository;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserCode;
import coffeeshout.user.domain.UserNickname;
import coffeeshout.user.domain.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public List<UserSearchResult> searchByNickname(Long myId, String rawNickname) {
        final List<User> users = userRepository.findAllByNickname(new UserNickname(rawNickname)).stream()
                .filter(user -> !user.getId().equals(myId))
                .toList();
        return toSearchResults(myId, users);
    }

    private List<UserSearchResult> toSearchResults(Long myId, List<User> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        final List<Long> userIds = users.stream().map(User::getId).toList();
        final Map<Long, Friendship> friendshipByUserId = friendshipRepository.findAllBetween(myId, userIds).stream()
                .collect(Collectors.toMap(
                        f -> f.counterpartOf(myId),
                        f -> f,
                        (a, b) -> a
                ));
        return users.stream()
                .map(user -> {
                    final Friendship friendship = friendshipByUserId.get(user.getId());
                    final RelationStatus status = friendship == null
                            ? RelationStatus.NONE
                            : friendship.statusFrom(myId);
                    return new UserSearchResult(user, status);
                })
                .toList();
    }

    private UserSearchResult toSearchResult(Long myId, User user) {
        return friendshipRepository.findBetween(myId, user.getId())
                .map(f -> new UserSearchResult(user, f.statusFrom(myId)))
                .orElse(new UserSearchResult(user, RelationStatus.NONE));
    }
}
