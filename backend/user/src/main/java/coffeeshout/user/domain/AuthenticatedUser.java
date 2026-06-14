package coffeeshout.user.domain;

import java.util.Objects;

public record AuthenticatedUser(
        Long userId,
        String userCode
) {
    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(userCode, "userCode는 null일 수 없습니다.");
    }
}
