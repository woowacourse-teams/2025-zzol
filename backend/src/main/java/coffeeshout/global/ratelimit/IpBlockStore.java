package coffeeshout.global.ratelimit;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 IP 차단 저장소.
 * <p>
 * 두 가지 방식으로 IP를 차단한다:
 * <ul>
 *   <li>즉시 차단: 악성 경로 접근 시 {@link #blockImmediately(String)} 호출</li>
 *   <li>누적 차단: 404 응답이 임계값 이상 발생한 IP를 {@link #incrementNotFoundAndBlockIfExceeded(String)} 로 관리</li>
 * </ul>
 *
 * <p>Redis Key 설계:
 * <ul>
 *   <li>{@code block:404:{ip}} — 404 누적 카운터, TTL: 1시간 (Fixed Window)</li>
 *   <li>{@code block:ip:{ip}} — 차단 마킹, TTL: 24시간</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpBlockStore {

    /**
     * INCR와 최초 생성 시 EXPIRE를 원자적으로 수행하는 Lua 스크립트.
     * TTL이 없는 카운터 키가 남아 영구 누적되는 경우를 방지한다.
     */
    private static final RedisScript<Long> INCREMENT_WITH_EXPIRE_SCRIPT = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private static final String NOT_FOUND_COUNTER_PREFIX = "block:404:";
    private static final String BLOCKED_IP_PREFIX = "block:ip:";
    private static final int NOT_FOUND_THRESHOLD = 5;
    private static final Duration NOT_FOUND_WINDOW = Duration.ofHours(1);
    private static final Duration BLOCK_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    public boolean isBlocked(String ip) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLOCKED_IP_PREFIX + ip));
    }

    public void blockImmediately(String ip) {
        stringRedisTemplate.opsForValue().set(BLOCKED_IP_PREFIX + ip, "1", BLOCK_TTL);
        log.warn("IP 차단 등록: ip={} ttl={}h", ip, BLOCK_TTL.toHours());
    }

    /**
     * 404 카운터를 증가시키고, 임계값 이상이면 IP를 차단한다.
     * Lua 스크립트로 INCR와 최초 EXPIRE를 원자적으로 수행해 TTL 누락을 방지한다.
     */
    public void incrementNotFoundAndBlockIfExceeded(String ip) {
        final String key = NOT_FOUND_COUNTER_PREFIX + ip;
        final Long count = stringRedisTemplate.execute(
                INCREMENT_WITH_EXPIRE_SCRIPT,
                List.of(key),
                String.valueOf(NOT_FOUND_WINDOW.getSeconds())
        );
        if (count == null) {
            log.warn("Redis 스크립트 실행 결과가 null입니다. Redis 연결 상태를 확인하세요: ip={}", ip);
            return;
        }
        if (count >= NOT_FOUND_THRESHOLD) {
            log.warn("404 임계값 초과 → IP 차단: ip={} count={}", ip, count);
            blockImmediately(ip);
        }
    }
}
