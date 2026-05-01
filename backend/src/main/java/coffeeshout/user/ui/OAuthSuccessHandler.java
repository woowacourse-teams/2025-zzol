package coffeeshout.user.ui;

import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.application.service.UserRegistrationService;
import coffeeshout.user.domain.OAuthProvider;
import coffeeshout.user.domain.User;
import coffeeshout.user.infra.oauth.CustomOAuth2UserService.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRegistrationService userRegistrationService;
    private final AuthTokenService authTokenService;

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

        final String code = authTokenService.issueCode(user);
        response.sendRedirect(frontendRedirectUri + "?code=" + code);
    }
}
