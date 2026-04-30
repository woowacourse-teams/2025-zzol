package coffeeshout.user.infra.oauth;

import coffeeshout.user.domain.OAuthProvider;
import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class NaverOAuth2UserConverter implements OAuth2UserConverter {

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.NAVER;
    }

    @Override
    public String extractProviderUserId(OAuth2User oAuth2User) {
        final Map<String, Object> response = oAuth2User.getAttribute("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("id");
    }

    @Override
    public String extractEmail(OAuth2User oAuth2User) {
        final Map<String, Object> response = oAuth2User.getAttribute("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("email");
    }

    @Override
    public String extractNickname(OAuth2User oAuth2User) {
        final Map<String, Object> response = oAuth2User.getAttribute("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("nickname");
    }
}
