package coffeeshout.user.domain;

public record OAuthCodeEntry(TokenPair tokenPair, boolean isNewUser) {
}
