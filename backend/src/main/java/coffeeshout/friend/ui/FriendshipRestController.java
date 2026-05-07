package coffeeshout.friend.ui;

import coffeeshout.friend.application.PresenceTracker;
import coffeeshout.friend.application.service.FriendSearchService;
import coffeeshout.friend.application.service.FriendshipService;
import coffeeshout.friend.domain.Friendship;
import coffeeshout.friend.ui.request.SendFriendRequestRequest;
import coffeeshout.friend.ui.response.AcceptFriendResponse;
import coffeeshout.friend.ui.response.FriendRequestResponse;
import coffeeshout.friend.ui.response.FriendResponse;
import coffeeshout.friend.ui.response.SendFriendRequestResponse;
import coffeeshout.friend.ui.response.UserSearchResponse;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.application.service.UserProfileService;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.exception.UserErrorCode;
import coffeeshout.user.ui.resolver.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class FriendshipRestController extends AuthenticatedController {

    private final FriendshipService friendshipService;
    private final FriendSearchService friendSearchService;
    private final UserProfileService userProfileService;
    private final PresenceTracker presenceTracker;

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> search(
            @AuthUser Optional<AuthenticatedUser> authUser,
            @RequestParam(required = false) String userCode,
            @RequestParam(required = false) String nickname) {
        final AuthenticatedUser me = requireAuthenticated(authUser);

        final List<UserSearchResponse> results;
        if (userCode != null) {
            results = friendSearchService.searchByUserCode(me.userId(), userCode).stream()
                    .map(r -> UserSearchResponse.from(r, presenceTracker))
                    .toList();
        } else if (nickname != null) {
            results = friendSearchService.searchByNickname(me.userId(), nickname).stream()
                    .map(r -> UserSearchResponse.from(r, presenceTracker))
                    .toList();
        } else {
            results = List.of();
        }
        return ResponseEntity.ok(results);
    }

    @PostMapping("/me/friends/requests")
    public ResponseEntity<SendFriendRequestResponse> sendRequest(
            @AuthUser Optional<AuthenticatedUser> authUser,
            @Valid @RequestBody SendFriendRequestRequest request) {
        final AuthenticatedUser me = requireAuthenticated(authUser);
        final Friendship friendship = friendshipService.sendRequest(me.userId(), request.targetUserId());
        return ResponseEntity.ok(SendFriendRequestResponse.from(friendship));
    }

    @GetMapping("/me/friends/requests/received")
    public ResponseEntity<List<FriendRequestResponse>> getReceivedRequests(
            @AuthUser Optional<AuthenticatedUser> authUser) {
        final AuthenticatedUser me = requireAuthenticated(authUser);
        final List<FriendRequestResponse> responses = friendshipService.findReceivedPending(me.userId()).stream()
                .map(FriendRequestResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/me/friends/requests/sent")
    public ResponseEntity<List<FriendRequestResponse>> getSentRequests(
            @AuthUser Optional<AuthenticatedUser> authUser) {
        final AuthenticatedUser me = requireAuthenticated(authUser);
        final List<FriendRequestResponse> responses = friendshipService.findSentPending(me.userId()).stream()
                .map(FriendRequestResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/me/friends/requests/{requestId}/accept")
    public ResponseEntity<AcceptFriendResponse> accept(
            @AuthUser Optional<AuthenticatedUser> authUser,
            @PathVariable Long requestId) {
        final AuthenticatedUser me = requireAuthenticated(authUser);
        final Friendship accepted = friendshipService.accept(me.userId(), requestId);
        final var counterpart = userProfileService.findById(accepted.counterpartOf(me.userId()));
        return ResponseEntity.ok(AcceptFriendResponse.from(counterpart));
    }

    @PostMapping("/me/friends/requests/{requestId}/reject")
    public ResponseEntity<Void> reject(
            @AuthUser Optional<AuthenticatedUser> authUser,
            @PathVariable Long requestId) {
        final AuthenticatedUser me = requireAuthenticated(authUser);
        friendshipService.reject(me.userId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/friends")
    public ResponseEntity<List<FriendResponse>> getFriends(
            @AuthUser Optional<AuthenticatedUser> authUser) {
        final AuthenticatedUser me = requireAuthenticated(authUser);
        final List<FriendResponse> responses = friendshipService.findFriends(me.userId()).stream()
                .map(f -> FriendResponse.from(f, presenceTracker))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/me/friends/{friendUserId}")
    public ResponseEntity<Void> unfriend(
            @AuthUser Optional<AuthenticatedUser> authUser,
            @PathVariable Long friendUserId) {
        final AuthenticatedUser me = requireAuthenticated(authUser);
        friendshipService.unfriend(me.userId(), friendUserId);
        return ResponseEntity.noContent().build();
    }

}
