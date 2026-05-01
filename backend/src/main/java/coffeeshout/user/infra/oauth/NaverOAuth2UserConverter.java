package coffeeshout.user.infra.oauth;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.exception.UserErrorCode;
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
        return (String) getResponse(oAuth2User).get("id");
    }

    @Override
    public String extractEmail(OAuth2User oAuth2User) {
        return (String) getResponse(oAuth2User).get("email");
    }

    @Override
    public String extractNickname(OAuth2User oAuth2User) {
        return (String) getResponse(oAuth2User).get("nickname");
    }

    private Map<String, Object> getResponse(OAuth2User oAuth2User) {
        final Map<String, Object> response = oAuth2User.getAttribute("response");
        if (response == null) {
            throw new BusinessException(UserErrorCode.OAUTH_PROVIDER_ERROR,
                    "네이버 OAuth2 응답에서 필수 정보를 가져올 수 없습니다.");
        }
        return response;
    }
}
