package coffeeshout.user.infra.oauth;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.exception.UserErrorCode;
import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class KakaoOAuth2UserConverter implements OAuth2UserConverter {

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public String extractProviderUserId(OAuth2User oAuth2User) {
        final Object id = oAuth2User.getAttribute("id");
        if (id == null) {
            throw new BusinessException(UserErrorCode.OAUTH_PROVIDER_ERROR, "카카오 사용자 ID를 가져올 수 없습니다.");
        }
        return String.valueOf(id);
    }

    @Override
    public String extractEmail(OAuth2User oAuth2User) {
        final Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
        if (kakaoAccount == null) {
            return null;
        }
        return (String) kakaoAccount.get("email");
    }

    @Override
    public String extractNickname(OAuth2User oAuth2User) {
        final Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
        if (kakaoAccount == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        final Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile == null) {
            return null;
        }
        return (String) profile.get("nickname");
    }
}
