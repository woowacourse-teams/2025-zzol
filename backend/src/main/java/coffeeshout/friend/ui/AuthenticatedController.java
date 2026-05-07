package coffeeshout.friend.ui;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.exception.UserErrorCode;
import java.util.Optional;

public abstract class AuthenticatedController {

    protected AuthenticatedUser requireAuthenticated(Optional<AuthenticatedUser> authUser) {
        return authUser.orElseThrow(() -> new BusinessException(
                UserErrorCode.UNAUTHORIZED, "인증이 필요합니다."));
    }
}
