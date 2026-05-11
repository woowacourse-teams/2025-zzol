package coffeeshout.global.websocket.auth;

import coffeeshout.global.exception.custom.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JjwtRoomSessionTokenIssuer implements RoomSessionTokenIssuer {

    private static final String CLAIM_PLAYER_NAME = "playerName";
    private static final String CLAIM_USER_ID = "userId";

    private final SecretKey secretKey;
    private final long expirationMillis;

    public JjwtRoomSessionTokenIssuer(
            RoomSessionTokenProperties properties,
            @Value("${room.removalDelay}") Duration roomRemovalDelay) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = roomRemovalDelay.toMillis();
    }

    @Override
    public String issue(RoomSessionClaim claim) {
        final long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(claim.joinCode())
                .claim(CLAIM_PLAYER_NAME, claim.playerName())
                .claim(CLAIM_USER_ID, claim.userId())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMillis))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public RoomSessionClaim verify(String token) {
        try {
            final Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            final String joinCode = claims.getSubject();
            final String playerName = claims.get(CLAIM_PLAYER_NAME, String.class);
            final Long userId = claims.get(CLAIM_USER_ID, Long.class);
            return RoomSessionClaim.of(joinCode, playerName, userId);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(RoomSessionTokenErrorCode.ROOM_TOKEN_EXPIRED, "만료된 Room Session Token입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(RoomSessionTokenErrorCode.ROOM_TOKEN_INVALID, "유효하지 않은 Room Session Token입니다.");
        }
    }
}
