package coffeeshout.user.ui;

import coffeeshout.user.config.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieHelper {

    public static final String COOKIE_NAME = "refreshToken";

    private final JwtProperties jwtProperties;

    public void set(HttpServletResponse response, String token) {
        final ResponseCookie cookie = base(token)
                .maxAge(Duration.ofSeconds(jwtProperties.refreshTokenExpirationSeconds()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        final ResponseCookie cookie = base("").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None");
    }
}
