package coffeeshout.friend.ui.response;

import coffeeshout.friend.application.PresenceTracker;
import coffeeshout.friend.application.service.UserSearchResult;

public record UserSearchResponse(
        Long userId,
        String userCode,
        String nickname,
        String relationStatus,
        boolean online
) {
    public static UserSearchResponse from(UserSearchResult result, PresenceTracker presenceTracker) {
        return new UserSearchResponse(
                result.user().getId(),
                result.user().getUserCode().value(),
                result.user().getNickname().value(),
                result.relationStatus().name(),
                presenceTracker.isOnline(result.user().getId())
        );
    }
}
