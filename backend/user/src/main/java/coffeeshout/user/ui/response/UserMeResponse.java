package coffeeshout.user.ui.response;

import coffeeshout.user.domain.User;

public record UserMeResponse(
        String userCode,
        String nickname,
        String provider
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getUserCode().value(),
                user.getNickname().value(),
                user.getOAuthAccount().provider().getRegistrationId()
        );
    }
}
