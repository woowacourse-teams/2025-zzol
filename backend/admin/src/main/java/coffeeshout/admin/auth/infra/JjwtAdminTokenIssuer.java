package coffeeshout.admin.auth.infra;

import coffeeshout.admin.account.domain.AdminAccountErrorCode;
import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.auth.AdminAuthProperties;
import coffeeshout.admin.auth.domain.AdminPrincipal;
import coffeeshout.admin.auth.domain.AdminTokenIssuer;
import coffeeshout.global.exception.custom.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * 관리자 액세스 토큰. HS256, 리프레시 없음.
 *
 * <p>리프레시 토큰을 두지 않는다. 관리자 세션은 짧게 끊기는 편이 안전하고, 만료되면
 * 구글 로그인을 다시 태우면 된다. 브라우저에 이미 구글 세션이 있으므로 재로그인 비용이 거의 없다.
 *
 * <p>{@code type=ADMIN} 클레임을 반드시 확인한다. 관리자 시크릿이 사용자 JWT 시크릿으로
 * 폴백될 수 있어(설정 참조), 같은 키로 서명된 사용자 토큰이 관리자 토큰으로 통과하면 안 된다.
 */
@Component
public class JjwtAdminTokenIssuer implements AdminTokenIssuer {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ADMIN = "ADMIN";

    private final SecretKey secretKey;
    private final long validityMillis;
    private final Clock clock;

    public JjwtAdminTokenIssuer(AdminAuthProperties properties, Clock clock) {
        this.secretKey = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.validityMillis = properties.tokenValiditySeconds() * 1000L;
        this.clock = clock;
    }

    @Override
    public String issue(AdminEmail email) {
        final long now = clock.millis();
        return Jwts.builder()
                .subject(email.value())
                .claim(CLAIM_TYPE, TYPE_ADMIN)
                .issuedAt(new Date(now))
                .expiration(new Date(now + validityMillis))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public AdminPrincipal verify(String token) {
        try {
            final Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .clock(() -> new Date(clock.millis()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TYPE_ADMIN.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new BusinessException(AdminAccountErrorCode.ADMIN_TOKEN_INVALID, "관리자 토큰이 아닙니다.");
            }
            return new AdminPrincipal(AdminEmail.parse(claims.getSubject())
                    .orElseThrow(() ->
                            new BusinessException(AdminAccountErrorCode.ADMIN_TOKEN_INVALID, "주체가 없는 관리자 토큰입니다.")));
        } catch (ExpiredJwtException e) {
            throw new BusinessException(AdminAccountErrorCode.ADMIN_TOKEN_EXPIRED, "만료된 관리자 토큰입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AdminAccountErrorCode.ADMIN_TOKEN_INVALID, "유효하지 않은 관리자 토큰입니다.");
        }
    }
}
