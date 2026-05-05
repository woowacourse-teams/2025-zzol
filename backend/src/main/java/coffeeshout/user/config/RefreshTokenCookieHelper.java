package coffeeshout.user.config;

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
        final ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofSeconds(jwtProperties.refreshTokenExpirationSeconds()))
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        final ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
