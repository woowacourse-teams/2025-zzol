package coffeeshout.user.domain.repository;

import coffeeshout.user.domain.AuthenticatedUser;
import java.util.Optional;

public interface RefreshTokenRepository {

    void save(Long userId, String userCode, String tokenId, long expirationSeconds);

    Optional<AuthenticatedUser> findByTokenId(String tokenId);

    void delete(String tokenId);

    void deleteAllByUserId(Long userId);
}
