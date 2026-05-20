package coffeeshout.user.infra.oauth;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.exception.UserErrorCode;
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
        final Object sub = oAuth2User.getAttribute("sub");
        if (sub == null) {
            throw new BusinessException(UserErrorCode.OAUTH_PROVIDER_ERROR, "Google 사용자 ID를 가져올 수 없습니다.");
        }
        return String.valueOf(sub);
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
