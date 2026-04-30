package coffeeshout.user.domain;

public record AuthenticatedUser(
        Long userId,
        String userCode
) {
}
