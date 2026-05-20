package coffeeshout.user.ui;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.domain.OAuthCodeEntry;
import coffeeshout.user.domain.TokenPair;
import coffeeshout.user.exception.UserErrorCode;
import coffeeshout.user.ui.request.ExchangeCodeRequest;
import coffeeshout.user.ui.resolver.AuthUser;
import coffeeshout.user.ui.response.AuthTokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthTokenService authTokenService;
    private final RefreshTokenCookieHelper cookieHelper;

    @PostMapping("/token")
    public ResponseEntity<AuthTokenResponse> exchangeCode(
            @Valid @RequestBody ExchangeCodeRequest request,
            HttpServletResponse response
    ) {
        final OAuthCodeEntry entry = authTokenService.exchangeCode(request.code());
        cookieHelper.set(response, entry.tokenPair().refreshToken());
        return ResponseEntity.ok(new AuthTokenResponse(entry.tokenPair().accessToken(), null, entry.isNewUser()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        final String refreshToken = extractRefreshTokenCookie(request);
        final TokenPair tokens = authTokenService.rotate(refreshToken);
        cookieHelper.set(response, tokens.refreshToken());
        return ResponseEntity.ok(new AuthTokenResponse(tokens.accessToken(), null, false));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthUser Optional<AuthenticatedUser> authUser,
            HttpServletResponse response
    ) {
        authUser.ifPresent(user -> authTokenService.revoke(user.userId()));
        cookieHelper.clear(response);
        return ResponseEntity.noContent().build();
    }

    private String extractRefreshTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new BusinessException(UserErrorCode.REFRESH_TOKEN_NOT_FOUND, "리프레시 토큰이 없습니다.");
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> RefreshTokenCookieHelper.COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        UserErrorCode.REFRESH_TOKEN_NOT_FOUND, "리프레시 토큰이 없습니다."));
    }
}
