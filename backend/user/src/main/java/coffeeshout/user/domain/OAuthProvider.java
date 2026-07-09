package coffeeshout.user.domain;

import coffeeshout.global.exception.custom.BusinessException;
import java.util.Optional;

public enum OAuthProvider {

    GOOGLE("google"),
    KAKAO("kakao"),
    NAVER("naver");

    private final String registrationId;

    OAuthProvider(String registrationId) {
        this.registrationId = registrationId;
    }

    public static OAuthProvider from(String registrationId) {
        return fromRegistrationId(registrationId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED,
                        "지원하지 않는 OAuth 제공자입니다: " + registrationId));
    }

    /**
     * 검증되지 않은 입력(예: URL 세그먼트)으로 provider를 안전하게 조회한다.
     * 미지원 값이면 비어 있는 Optional을 반환한다 — 예외를 던지지 않으므로 메트릭 태그 등
     * 무검증 외부 입력을 다루는 경로에서 카디널리티 폭발 없이 화이트리스트로 쓸 수 있다.
     */
    public static Optional<OAuthProvider> fromRegistrationId(String registrationId) {
        for (OAuthProvider provider : values()) {
            if (provider.registrationId.equalsIgnoreCase(registrationId)) {
                return Optional.of(provider);
            }
        }
        return Optional.empty();
    }

    public String getRegistrationId() {
        return registrationId;
    }
}
