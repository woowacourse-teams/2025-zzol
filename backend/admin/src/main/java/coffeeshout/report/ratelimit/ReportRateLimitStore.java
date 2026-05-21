package coffeeshout.report.ratelimit;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis(Redisson) 기반 신고 제출 Rate Limit 저장소.
 * <p>
 * Redisson {@link RRateLimiter}(토큰 버킷)를 사용하여 IP당 제출 횟수를 제한한다.
 *
 * <p>Redis Key 설계:
 * <ul>
 *   <li>{@code report:submit:{ip}} — RRateLimiter 메타 키 (Redisson 내부 관리), TTL: {@code report.rate-limit.ttl}</li>
 * </ul>
 *
 * <p>Redis 장애 시 서킷 브레이커가 열리며 fail-open으로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(ReportRateLimitProperties.class)
public class ReportRateLimitStore {

    private static final String KEY_PREFIX = "report:submit:";

    private final RedissonClient redissonClient;
    private final ReportRateLimitProperties properties;
    private final MeterRegistry meterRegistry;

    private Counter droppedCounter;

    @PostConstruct
    private void initMetrics() {
        droppedCounter = Counter.builder("report.ratelimit.dropped.total")
                .description("신고 Rate Limit에 의해 거절된 요청 수")
                .register(meterRegistry);
    }

    /**
     * 토큰을 획득 시도한다. 한도 초과 시 {@code false}를 반환한다.
     *
     * @return 제출 가능하면 {@code true}, 한도 초과면 {@code false}
     */
    @CircuitBreaker(name = "reportRateLimiter", fallbackMethod = "tryAcquireFallback")
    public boolean tryAcquire(String ip) {
        final RRateLimiter rateLimiter = redissonClient.getRateLimiter(KEY_PREFIX + ip);
        boolean isNew = rateLimiter.trySetRate(RateType.OVERALL, properties.rate(), properties.rateInterval(), properties.rateIntervalUnit());
        if (isNew) {
            rateLimiter.expire(properties.ttl());
        }
        boolean acquired = rateLimiter.tryAcquire();
        if (!acquired) {
            droppedCounter.increment();
        }
        return acquired;
    }

    private boolean tryAcquireFallback(String ip, Throwable t) {
        log.warn("서킷 브레이커 OPEN/장애 발생: Redis 장애로 신고 rate limit을 확인할 수 없습니다. Fail-open 처리합니다. ip={} error={}", ip, t.getMessage());
        return true;
    }
}
