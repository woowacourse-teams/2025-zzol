package coffeeshout.user.domain;

public record OAuthAccount(
        OAuthProvider provider,
        String providerUserId,
        String email
) {
}
