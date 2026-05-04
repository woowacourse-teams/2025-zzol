package coffeeshout.user.infra.redis;

import coffeeshout.user.domain.OAuthCodeEntry;
import coffeeshout.user.domain.TokenPair;
import coffeeshout.user.domain.repository.OAuthCodeRepository;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisOAuthCodeRepository implements OAuthCodeRepository {

    private static final String KEY_PREFIX = "oauth:code:";
    private static final String DELIMITER = "||";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(String code, TokenPair tokens, boolean isNewUser, long ttlSeconds) {
        final String value = tokens.accessToken() + DELIMITER + tokens.refreshToken() + DELIMITER + isNewUser;
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + code, value, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<OAuthCodeEntry> findAndDelete(String code) {
        final String key = KEY_PREFIX + code;
        final String value = stringRedisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            return Optional.empty();
        }
        final String[] parts = value.split("\\|\\|", 3);
        if (parts.length != 3) {
            return Optional.empty();
        }
        final TokenPair tokenPair = new TokenPair(parts[0], parts[1]);
        final boolean isNewUser = Boolean.parseBoolean(parts[2]);
        return Optional.of(new OAuthCodeEntry(tokenPair, isNewUser));
    }
}
