package coffeeshout.user.application.service;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.user.config.JwtProperties;
import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.RefreshTokenRepository;
import coffeeshout.user.domain.service.JwtIssuer;
import coffeeshout.user.exception.UserErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtIssuer jwtIssuer;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public record TokenPair(String accessToken, String refreshToken) {
    }

    public TokenPair issue(User user) {
        final AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(), user.getUserCode().value());
        final String accessToken = jwtIssuer.issue(authenticatedUser);
        final String refreshToken = generateRefreshToken(user.getId(), user.getUserCode().value());
        return new TokenPair(accessToken, refreshToken);
    }

    public TokenPair rotate(String refreshToken) {
        final String[] parts = parseRefreshToken(refreshToken);
        final long userId = Long.parseLong(parts[0]);
        final String tokenId = parts[1];

        final AuthenticatedUser stored = refreshTokenRepository.findByTokenId(tokenId)
                .orElseGet(() -> {
                    refreshTokenRepository.deleteAllByUserId(userId);
                    throw new BusinessException(
                            UserErrorCode.REFRESH_TOKEN_NOT_FOUND, "이미 사용된 리프레시 토큰입니다.");
                });

        refreshTokenRepository.delete(tokenId);

        final String newRefreshToken = generateRefreshToken(stored.userId(), stored.userCode());
        final String newAccessToken = jwtIssuer.issue(stored);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    public void revoke(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    public AuthenticatedUser verify(String accessToken) {
        return jwtIssuer.verify(accessToken);
    }

    private String generateRefreshToken(Long userId, String userCode) {
        final String tokenId = UUID.randomUUID().toString();
        refreshTokenRepository.save(userId, userCode, tokenId, jwtProperties.refreshTokenExpirationSeconds());
        return userId + ":" + tokenId;
    }

    private String[] parseRefreshToken(String refreshToken) {
        final String[] parts = refreshToken.split(":", 2);
        if (parts.length != 2) {
            throw new BusinessException(UserErrorCode.REFRESH_TOKEN_NOT_FOUND, "유효하지 않은 리프레시 토큰입니다.");
        }
        return parts;
    }
}
