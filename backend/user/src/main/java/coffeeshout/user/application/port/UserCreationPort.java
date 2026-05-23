package coffeeshout.user.application.port;

import coffeeshout.user.domain.OAuthAccount;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.UserNickname;

public interface UserCreationPort {
    User attempt(UserNickname nickname, OAuthAccount oAuthAccount);
}
