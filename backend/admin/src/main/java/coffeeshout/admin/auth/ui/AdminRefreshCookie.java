package coffeeshout.admin.auth.ui;

import coffeeshout.admin.auth.AdminAuthProperties;
import coffeeshout.admin.auth.domain.AdminRefreshToken;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 관리자 refresh 쿠키.
 *
 * <p>회원 쿠키({@code refreshToken}, {@code Path=/})와 이름을 다르게 둔다. 둘 다 api 호스트에
 * 붙으므로 이름이 같으면 서로 덮어쓴다.
 *
 * <p>{@code Path} 를 인증 경로로 좁힌다. 다른 관리자 API 에는 이 쿠키가 실리지 않아서, 그 API 들은
 * 여전히 Authorization 헤더로만 인증되고 CSRF 를 꺼 둔 근거도 그대로 유지된다.
 *
 * <p>{@code SameSite=Strict} 여도 admin.zzol.site 에서 api.zzol.site 로 실린다. 둘이 같은 사이트다.
 */
@Component
@RequiredArgsConstructor
public class AdminRefreshCookie {

    public static final String NAME = "zzol_admin_refresh";
    private static final String PATH = "/admin/api/auth";

    private final AdminAuthProperties adminAuthProperties;

    public void set(HttpServletResponse response, AdminRefreshToken token) {
        write(response, base(token.value()).maxAge(adminAuthProperties.refreshTokenValidity()));
    }

    public void clear(HttpServletResponse response) {
        write(response, base("").maxAge(0));
    }

    private static ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(true)
                .path(PATH)
                .sameSite("Strict");
    }

    private static void write(HttpServletResponse response, ResponseCookie.ResponseCookieBuilder cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
    }
}
