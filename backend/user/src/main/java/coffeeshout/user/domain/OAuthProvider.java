package coffeeshout.user.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.exception.UserErrorCode;

public enum OAuthProvider {

    GOOGLE("google"),
    KAKAO("kakao"),
    NAVER("naver");

    private final String registrationId;

    OAuthProvider(String registrationId) {
        this.registrationId = registrationId;
    }

    public static OAuthProvider from(String registrationId) {
        for (OAuthProvider provider : values()) {
            if (provider.registrationId.equalsIgnoreCase(registrationId)) {
                return provider;
            }
        }
        throw new BusinessException(UserErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED,
                "지원하지 않는 OAuth 제공자입니다: " + registrationId);
    }

    public String getRegistrationId() {
        return registrationId;
    }
}
