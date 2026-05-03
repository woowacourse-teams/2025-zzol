package coffeeshout.user.ui.response;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken
) {
}
