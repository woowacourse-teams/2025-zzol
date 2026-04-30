package coffeeshout.user.infra.oauth;

import coffeeshout.user.domain.OAuthProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuth2UserConverter {

    OAuthProvider provider();

    String extractProviderUserId(OAuth2User oAuth2User);

    String extractEmail(OAuth2User oAuth2User);

    String extractNickname(OAuth2User oAuth2User);
}
