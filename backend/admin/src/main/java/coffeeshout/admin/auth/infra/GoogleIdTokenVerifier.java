package coffeeshout.admin.auth.infra;

import coffeeshout.admin.account.domain.AdminAccountErrorCode;
import coffeeshout.admin.auth.AdminAuthProperties;
import coffeeshout.admin.auth.domain.SocialIdTokenVerifier;
import coffeeshout.global.exception.custom.BusinessException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * 구글 ID 토큰 검증.
 *
 * <p>서명(RS256), 발급자, 만료는 디코더가 처리하고, 여기서는 <b>대상(aud)</b>과
 * <b>이메일 검증 여부</b>를 확인한다.
 *
 * <p>{@code JwtDecoders.fromIssuerLocation()}을 쓰지 않는다. 그 메서드는 <b>빈 생성 시점에</b>
 * OpenID 디스커버리 문서를 받아오므로 앱 기동이 구글 네트워크에 묶인다. 구글이 잠깐 느리면
 * 배포가 실패하고, 테스트는 인터넷 없이 못 돈다. JWKS 주소를 직접 지정하면 키는
 * <b>첫 검증 때</b> 받아 캐싱하므로 기동 경로에서 외부 호출이 사라진다.
 */
@Slf4j
@Component
public class GoogleIdTokenVerifier implements SocialIdTokenVerifier {

    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    /**
     * 구글은 ID 토큰의 iss 를 두 형태로 발급해 왔다. 둘 다 받는다.
     * 하나만 허용하면 어느 날 다른 형태가 오는 순간 전원 로그인이 막힌다.
     */
    private static final Set<String> VALID_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_EMAIL_VERIFIED = "email_verified";

    private final JwtDecoder jwtDecoder;
    private final String clientId;

    /**
     * 생성자가 둘이라 어느 쪽으로 주입할지 명시해야 한다. 표시가 없으면 스프링이
     * 기본 생성자를 찾다가 기동에 실패한다.
     */
    @Autowired
    public GoogleIdTokenVerifier(AdminAuthProperties properties) {
        this(defaultDecoder(), properties.googleClientId());
    }

    GoogleIdTokenVerifier(JwtDecoder jwtDecoder, String clientId) {
        this.jwtDecoder = jwtDecoder;
        this.clientId = clientId;
    }

    private static JwtDecoder defaultDecoder() {
        final NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuerValidator()));
        return decoder;
    }

    private static OAuth2TokenValidator<Jwt> issuerValidator() {
        return jwt -> {
            final Object issuer = jwt.getClaim(JwtClaimNames.ISS);
            if (issuer != null && VALID_ISSUERS.contains(issuer.toString())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_issuer", "허용되지 않은 발급자입니다: " + issuer, null));
        };
    }

    @Override
    public String verifyAndExtractEmail(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new BusinessException(AdminAccountErrorCode.GOOGLE_ID_TOKEN_INVALID, "ID 토큰이 비어 있습니다.");
        }

        final Jwt jwt = decode(idToken);
        validateAudience(jwt);
        validateEmailVerified(jwt);

        final String email = jwt.getClaimAsString(CLAIM_EMAIL);
        if (email == null || email.isBlank()) {
            throw new BusinessException(AdminAccountErrorCode.GOOGLE_ID_TOKEN_INVALID, "ID 토큰에 이메일이 없습니다.");
        }
        return email;
    }

    private Jwt decode(String idToken) {
        try {
            return jwtDecoder.decode(idToken);
        } catch (JwtException e) {
            // 실패 사유를 응답에 담지 않는다. 어떤 검증에서 걸렸는지 알려주면 탐색을 돕는다.
            log.warn("구글 ID 토큰 검증 실패: {}", e.getMessage());
            throw new BusinessException(AdminAccountErrorCode.GOOGLE_ID_TOKEN_INVALID, "구글 인증에 실패했습니다.");
        }
    }

    private void validateAudience(Jwt jwt) {
        final List<String> audience = jwt.getClaimAsStringList(JwtClaimNames.AUD);
        if (audience == null || !audience.contains(clientId)) {
            // 다른 서비스용으로 발급된 구글 토큰을 여기 들고 오는 경로를 막는다.
            log.warn("구글 ID 토큰 대상 불일치: aud={}", audience);
            throw new BusinessException(AdminAccountErrorCode.GOOGLE_ID_TOKEN_INVALID, "구글 인증에 실패했습니다.");
        }
    }

    private void validateEmailVerified(Jwt jwt) {
        // 구글은 이 클레임을 boolean 으로도 문자열로도 보낸 이력이 있어 둘 다 받는다.
        final Object verified = jwt.getClaim(CLAIM_EMAIL_VERIFIED);
        final boolean isVerified = Boolean.TRUE.equals(verified) || "true".equals(verified);
        if (!isVerified) {
            throw new BusinessException(AdminAccountErrorCode.GOOGLE_EMAIL_UNVERIFIED, "이메일이 검증되지 않은 구글 계정입니다.");
        }
    }
}
