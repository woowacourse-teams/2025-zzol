package coffeeshout.admin.auth.infra;

import coffeeshout.admin.account.domain.AdminEmail;
import coffeeshout.admin.auth.domain.AdminRefreshToken;
import coffeeshout.admin.auth.domain.AdminRefreshTokenRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

/**
 * family 하나를 해시 키 하나({@code email}, {@code tokenId})로 둔다.
 *
 * <p>토큰마다 키를 두지 않는다. "현재 토큰 하나만 유효하고 나머지는 재사용"이라는 규칙이
 * 키 하나로 표현되고, 폐기도 DEL 한 번으로 끝난다.
 *
 * <p>회전은 Lua 로 한 번에 처리한다. 조회와 교체가 따로 나가면 같은 토큰으로 동시에 온
 * 두 요청이 둘 다 통과한다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisAdminRefreshTokenRepository implements AdminRefreshTokenRepository {

    private static final String KEY_PREFIX = "admin:refresh:family:";
    private static final String REUSED = "";

    private static final RedisScript<Long> SAVE_SCRIPT = RedisScript.of("""
            redis.call('HSET', KEYS[1], 'email', ARGV[1], 'tokenId', ARGV[2])
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            return 1
            """, Long.class);

    // 없으면 nil, 재사용이면 빈 문자열, 성공하면 이메일을 돌려준다.
    private static final RedisScript<String> ROTATE_SCRIPT = RedisScript.of("""
            local current = redis.call('HGET', KEYS[1], 'tokenId')
            if not current then
              return nil
            end
            if current ~= ARGV[1] then
              redis.call('DEL', KEYS[1])
              return ''
            end
            redis.call('HSET', KEYS[1], 'tokenId', ARGV[2])
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            return redis.call('HGET', KEYS[1], 'email')
            """, String.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(AdminRefreshToken token, AdminEmail email, Duration ttl) {
        stringRedisTemplate.execute(
                SAVE_SCRIPT, List.of(key(token.familyId())), email.value(), token.tokenId(), seconds(ttl));
    }

    @Override
    public Optional<AdminEmail> rotate(AdminRefreshToken presented, AdminRefreshToken next, Duration ttl) {
        final String result = stringRedisTemplate.execute(
                ROTATE_SCRIPT, List.of(key(presented.familyId())), presented.tokenId(), next.tokenId(), seconds(ttl));
        if (result == null) {
            return Optional.empty();
        }
        if (REUSED.equals(result)) {
            // 탈취를 의심할 신호다. 정상 흐름에서는 탭 사이 경합으로도 생길 수 있어 WARN 으로 둔다.
            log.warn("관리자 refresh 토큰 재사용 감지, family 폐기: familyId={}", presented.familyId());
            return Optional.empty();
        }
        return AdminEmail.parse(result);
    }

    @Override
    public void revoke(String familyId) {
        stringRedisTemplate.delete(key(familyId));
    }

    private static String key(String familyId) {
        return KEY_PREFIX + familyId;
    }

    private static String seconds(Duration ttl) {
        // 0 이하를 EXPIRE 에 넘기면 키가 즉시 사라진다. 조용히 로그인이 안 되는 것보다 여기서 터지는 편이 낫다.
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("refresh 토큰 TTL은 양수여야 합니다: " + ttl);
        }
        return String.valueOf(ttl.toSeconds());
    }
}
