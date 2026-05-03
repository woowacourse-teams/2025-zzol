package coffeeshout.user.infra.redis;

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
    public void save(String code, TokenPair tokens, long ttlSeconds) {
        final String value = tokens.accessToken() + DELIMITER + tokens.refreshToken();
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + code, value, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<TokenPair> findAndDelete(String code) {
        final String key = KEY_PREFIX + code;
        final String value = stringRedisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            return Optional.empty();
        }
        final int delimIdx = value.indexOf(DELIMITER);
        if (delimIdx < 0) {
            return Optional.empty();
        }
        final String accessToken = value.substring(0, delimIdx);
        final String refreshToken = value.substring(delimIdx + DELIMITER.length());
        return Optional.of(new TokenPair(accessToken, refreshToken));
    }
}
