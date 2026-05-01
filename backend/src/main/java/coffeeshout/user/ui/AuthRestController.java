package coffeeshout.user.ui;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.application.service.AuthTokenService.TokenPair;
import coffeeshout.user.config.JwtProperties;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.exception.UserErrorCode;
import coffeeshout.user.ui.resolver.AuthUser;
import coffeeshout.user.ui.response.AuthTokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthRestController {

    static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthTokenService authTokenService;
    private final JwtProperties jwtProperties;

    @PostMapping("/token")
    public ResponseEntity<AuthTokenResponse> exchangeCode(
            @RequestBody Map<String, String> body,
            HttpServletResponse response
    ) {
        final String code = body.get("code");
        if (code == null || code.isBlank()) {
            throw new BusinessException(UserErrorCode.OAUTH_CODE_NOT_FOUND, "인증 코드가 없습니다.");
        }
        final TokenPair tokens = authTokenService.exchangeCode(code);
        setRefreshTokenCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(new AuthTokenResponse(tokens.accessToken(), null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        final String refreshToken = extractRefreshTokenCookie(request);
        final TokenPair tokens = authTokenService.rotate(refreshToken);
        setRefreshTokenCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(new AuthTokenResponse(tokens.accessToken(), null));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthUser Optional<AuthenticatedUser> authUser) {
        authUser.ifPresent(user -> authTokenService.revoke(user.userId()));
        return ResponseEntity.noContent().build();
    }

    private String extractRefreshTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new BusinessException(UserErrorCode.REFRESH_TOKEN_NOT_FOUND, "리프레시 토큰이 없습니다.");
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        UserErrorCode.REFRESH_TOKEN_NOT_FOUND, "리프레시 토큰이 없습니다."));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        final ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofSeconds(jwtProperties.refreshTokenExpirationSeconds()))
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
