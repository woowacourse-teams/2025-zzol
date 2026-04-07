package coffeeshout.global.ratelimit;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private static final String NOT_FOUND_COUNTER_PREFIX = "block:404:";
    private static final String BLOCKED_IP_PREFIX = "block:ip:";
    private static final int NOT_FOUND_THRESHOLD = 5;
    private static final Duration NOT_FOUND_WINDOW = Duration.ofHours(1);
    private static final Duration BLOCK_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    public boolean isBlocked(final String ip) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLOCKED_IP_PREFIX + ip));
    }

    public void blockImmediately(final String ip) {
        stringRedisTemplate.opsForValue().set(BLOCKED_IP_PREFIX + ip, "1", BLOCK_TTL);
        log.warn("IP 차단 등록: ip={} ttl={}h", ip, BLOCK_TTL.toHours());
    }

    /**
     * 404 카운터를 증가시키고, 임계값 이상이면 IP를 차단한다.
     * 카운터 키가 처음 생성될 때 TTL을 설정해 Fixed Window를 구현한다.
     */
    public void incrementNotFoundAndBlockIfExceeded(final String ip) {
        final String key = NOT_FOUND_COUNTER_PREFIX + ip;
        final Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == null) {
            return;
        }
        if (count == 1) {
            stringRedisTemplate.expire(key, NOT_FOUND_WINDOW);
        }
        if (count >= NOT_FOUND_THRESHOLD) {
            log.warn("404 임계값 초과 → IP 차단: ip={} count={}", ip, count);
            blockImmediately(ip);
        }
    }
}
