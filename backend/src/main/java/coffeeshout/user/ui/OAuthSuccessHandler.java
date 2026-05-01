package coffeeshout.user.ui;

import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.application.service.UserRegistrationService;
import coffeeshout.user.config.JwtProperties;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.domain.User;
import coffeeshout.user.infra.oauth.CustomOAuth2UserService.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final UserRegistrationService userRegistrationService;
    private final AuthTokenService authTokenService;
    private final JwtProperties jwtProperties;

    @Value("${user.oauth.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        final CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        final OAuthProvider provider = OAuthProvider.from(oAuth2User.registrationId());

        final User user = userRegistrationService.registerOrLogin(
                provider,
                oAuth2User.providerUserId(),
                oAuth2User.email(),
                oAuth2User.nickname()
        );

        final AuthTokenService.TokenPair tokens = authTokenService.issue(user);

        final ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, tokens.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofSeconds(jwtProperties.refreshTokenExpirationSeconds()))
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        final String redirectUrl = frontendRedirectUri + "?accessToken=" + tokens.accessToken();
        response.sendRedirect(redirectUrl);
    }
}
