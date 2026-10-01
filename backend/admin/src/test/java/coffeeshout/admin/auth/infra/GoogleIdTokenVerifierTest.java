package coffeeshout.admin.auth.infra;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import coffeeshout.admin.account.domain.AdminAccountErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@DisplayName("GoogleIdTokenVerifier")
@ExtendWith(MockitoExtension.class)
class GoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "zzol-admin.apps.googleusercontent.com";
    private static final String TOKEN = "google-id-token";

    @Mock
    private JwtDecoder jwtDecoder;

    private GoogleIdTokenVerifier verifier() {
        return new GoogleIdTokenVerifier(jwtDecoder, CLIENT_ID);
    }

    private static Jwt jwt(Map<String, Object> claims) {
        final Map<String, Object> merged = new HashMap<>(Map.of(
                "iss",
                "https://accounts.google.com",
                "aud",
                List.of(CLIENT_ID),
                "email",
                "mj@zzol.site",
                "email_verified",
                true));
        merged.putAll(claims);
        return new Jwt(TOKEN, Instant.now(), Instant.now().plusSeconds(600), Map.of("alg", "RS256"), merged);
    }

    @Nested
    class 성공 {

        @Test
        void 검증에_성공하면_이메일을_돌려준다() {
            given(jwtDecoder.decode(TOKEN)).willReturn(jwt(Map.of()));

            assertThat(verifier().verifyAndExtractEmail(TOKEN)).isEqualTo("mj@zzol.site");
        }

        @Test
        void email_verified가_문자열_true여도_통과한다() {
            // 구글이 이 클레임을 문자열로 보낸 이력이 있다.
            given(jwtDecoder.decode(TOKEN)).willReturn(jwt(Map.of("email_verified", "true")));

            assertThat(verifier().verifyAndExtractEmail(TOKEN)).isEqualTo("mj@zzol.site");
        }

        @Test
        void aud가_여러_개여도_우리_클라이언트가_있으면_통과한다() {
            given(jwtDecoder.decode(TOKEN)).willReturn(jwt(Map.of("aud", List.of("other-client", CLIENT_ID))));

            assertThat(verifier().verifyAndExtractEmail(TOKEN)).isEqualTo("mj@zzol.site");
        }
    }

    @Nested
    class 실패 {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void 토큰이_비어_있으면_거부한다(String token) {
            assertCoffeeShoutException(
                    () -> verifier().verifyAndExtractEmail(token), AdminAccountErrorCode.GOOGLE_ID_TOKEN_INVALID);
        }

        @Test
        void 디코더가_거부하면_실패_사유를_감춘_메시지를_낸다() {
            willThrow(new BadJwtException("signature mismatch"))
                    .given(jwtDecoder)
                    .decode(TOKEN);

            assertCoffeeShoutException(
                    () -> verifier().verifyAndExtractEmail(TOKEN), AdminAccountErrorCode.GOOGLE_ID_TOKEN_INVALID);
        }

        @Test
        void 다른_클라이언트용_토큰은_거부한다() {
            // 다른 서비스에서 발급받은 유효한 구글 토큰을 들고 오는 경로를 막는다.
            given(jwtDecoder.decode(TOKEN))
                    .willReturn(jwt(Map.of("aud", List.of("someone-else.apps.googleusercontent.com"))));

            assertCoffeeShoutException(
                    () -> verifier().verifyAndExtractEmail(TOKEN), AdminAccountErrorCode.GOOGLE_ID_TOKEN_INVALID);
        }

        @Test
        void 이메일이_검증되지_않았으면_거부한다() {
            given(jwtDecoder.decode(TOKEN)).willReturn(jwt(Map.of("email_verified", false)));

            assertCoffeeShoutException(
                    () -> verifier().verifyAndExtractEmail(TOKEN), AdminAccountErrorCode.GOOGLE_EMAIL_UNVERIFIED);
        }

        @Test
        void email_verified_클레임이_없으면_거부한다() {
            final Jwt withoutClaim = new Jwt(
                    TOKEN,
                    Instant.now(),
                    Instant.now().plusSeconds(600),
                    Map.of("alg", "RS256"),
                    Map.of("iss", "https://accounts.google.com", "aud", List.of(CLIENT_ID), "email", "mj@zzol.site"));
            given(jwtDecoder.decode(TOKEN)).willReturn(withoutClaim);

            assertCoffeeShoutException(
                    () -> verifier().verifyAndExtractEmail(TOKEN), AdminAccountErrorCode.GOOGLE_EMAIL_UNVERIFIED);
        }

        @Test
        void 이메일_클레임이_없으면_거부한다() {
            final Jwt withoutEmail = new Jwt(
                    TOKEN,
                    Instant.now(),
                    Instant.now().plusSeconds(600),
                    Map.of("alg", "RS256"),
                    Map.of("iss", "https://accounts.google.com", "aud", List.of(CLIENT_ID), "email_verified", true));
            given(jwtDecoder.decode(TOKEN)).willReturn(withoutEmail);

            assertCoffeeShoutException(
                    () -> verifier().verifyAndExtractEmail(TOKEN), AdminAccountErrorCode.GOOGLE_ID_TOKEN_INVALID);
        }
    }
}
