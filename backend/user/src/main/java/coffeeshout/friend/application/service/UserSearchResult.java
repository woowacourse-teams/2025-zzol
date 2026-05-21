package coffeeshout.friend.application.service;

import coffeeshout.user.domain.User;

public record UserSearchResult(User user, RelationStatus relationStatus) {
}
