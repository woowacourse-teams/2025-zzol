package coffeeshout.user.application.service;

import coffeeshout.user.domain.User;

public record LoginResult(User user, boolean isNewUser) {
}
