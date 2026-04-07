package coffeeshout.global.ratelimit;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * Redis(Redisson) 기반 신고 제출 Rate Limit 저장소.
 * <p>
 * Redisson {@link RRateLimiter}(토큰 버킷)를 사용하여 IP당 제출 횟수를 제한한다.
 *
 * <p>Redis Key 설계:
 * <ul>
 *   <li>{@code report:submit:{ip}} — RRateLimiter 메타 키 (Redisson 내부 관리)</li>
 * </ul>
 *
 * <p>Redis 장애 시 서킷 브레이커가 열리며 fail-open으로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportRateLimitStore {

    private static final String KEY_PREFIX = "report:submit:";
    private static final long RATE = 5;
    private static final long RATE_INTERVAL = 1;
    private static final RateIntervalUnit RATE_INTERVAL_UNIT = RateIntervalUnit.HOURS;

    private final RedissonClient redissonClient;

    /**
     * 토큰을 획득 시도한다. 한도 초과 시 {@code false}를 반환한다.
     *
     * @return 제출 가능하면 {@code true}, 한도 초과면 {@code false}
     */
    @CircuitBreaker(name = "reportRateLimiter", fallbackMethod = "tryAcquireFallback")
    public boolean tryAcquire(String ip) {
        final RRateLimiter rateLimiter = redissonClient.getRateLimiter(KEY_PREFIX + ip);
        rateLimiter.trySetRate(RateType.OVERALL, RATE, RATE_INTERVAL, RATE_INTERVAL_UNIT);
        return rateLimiter.tryAcquire();
    }

    private boolean tryAcquireFallback(String ip, Throwable t) {
        log.warn("서킷 브레이커 OPEN/장애 발생: Redis 장애로 신고 rate limit을 확인할 수 없습니다. Fail-open 처리합니다. ip={} error={}", ip, t.getMessage());
        return true;
    }
}
