package coffeeshout.friend.application.service;

import coffeeshout.friend.domain.RelationStatus;
import coffeeshout.user.domain.User;

public record UserSearchResult(User user, RelationStatus relationStatus) {}
