package coffeeshout.user.infra.jwt;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.config.JwtProperties;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.domain.UserErrorCode;
import coffeeshout.user.domain.service.JwtIssuer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JjwtIssuer implements JwtIssuer {

    private static final String CLAIM_USER_CODE = "userCode";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;

    public JjwtIssuer(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMillis = properties.accessTokenExpirationSeconds() * 1000L;
    }

    @Override
    public String issue(AuthenticatedUser user) {
        final long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim(CLAIM_USER_CODE, user.userCode())
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpirationMillis))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public AuthenticatedUser verify(String token) {
        try {
            final Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            final Long userId = Long.parseLong(claims.getSubject());
            final String userCode = claims.get(CLAIM_USER_CODE, String.class);
            return new AuthenticatedUser(userId, userCode);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(UserErrorCode.TOKEN_EXPIRED, "만료된 액세스 토큰입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "유효하지 않은 액세스 토큰입니다.");
        }
    }
}
