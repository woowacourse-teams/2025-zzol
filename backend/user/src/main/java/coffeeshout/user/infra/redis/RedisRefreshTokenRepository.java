package coffeeshout.user.infra.redis;

import coffeeshout.user.domain.AuthenticatedUser;
import coffeeshout.user.domain.repository.RefreshTokenRepository;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String TOKEN_KEY_PREFIX = "refresh:token:";
    private static final String USER_TOKENS_KEY_PREFIX = "refresh:user:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(Long userId, String userCode, String tokenId, long expirationSeconds) {
        final String tokenKey = tokenKey(tokenId);
        final String userTokensKey = userTokensKey(userId);
        final String storedValue = userId + ":" + userCode;

        stringRedisTemplate.opsForValue().set(tokenKey, storedValue, expirationSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.opsForSet().add(userTokensKey, tokenId);
        stringRedisTemplate.expire(userTokensKey, expirationSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<AuthenticatedUser> findByTokenId(String tokenId) {
        final String value = stringRedisTemplate.opsForValue().get(tokenKey(tokenId));
        if (value == null) {
            return Optional.empty();
        }
        final String[] parts = value.split(":", 2);
        return Optional.of(new AuthenticatedUser(Long.parseLong(parts[0]), parts[1]));
    }

    @Override
    public void delete(String tokenId) {
        stringRedisTemplate.delete(tokenKey(tokenId));
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        final Set<String> tokenIds = stringRedisTemplate.opsForSet().members(userTokensKey(userId));
        if (tokenIds == null || tokenIds.isEmpty()) {
            return;
        }
        tokenIds.forEach(tokenId -> stringRedisTemplate.delete(tokenKey(tokenId)));
        stringRedisTemplate.delete(userTokensKey(userId));
    }

    private String tokenKey(String tokenId) {
        return TOKEN_KEY_PREFIX + tokenId;
    }

    private String userTokensKey(Long userId) {
        return USER_TOKENS_KEY_PREFIX + userId;
    }
}
