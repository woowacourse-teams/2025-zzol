package coffeeshout.user.infra.oauth;

import coffeeshout.user.domain.OAuthProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuth2UserConverter implements OAuth2UserConverter {

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public String extractProviderUserId(OAuth2User oAuth2User) {
        return oAuth2User.getAttribute("sub");
    }

    @Override
    public String extractEmail(OAuth2User oAuth2User) {
        return oAuth2User.getAttribute("email");
    }

    @Override
    public String extractNickname(OAuth2User oAuth2User) {
        return oAuth2User.getAttribute("name");
    }
}
