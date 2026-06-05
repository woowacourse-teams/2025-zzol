package coffeeshout.global.ipblock;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 IP 차단 저장소.
 * <p>
 * 두 가지 방식으로 IP를 차단한다:
 * <ul>
 *   <li>즉시 차단: 악성 경로 접근 시 {@link #blockImmediately(Ip)} 호출</li>
 *   <li>누적 차단: 404 응답이 임계값 이상 발생한 IP를 {@link #incrementNotFoundAndBlockIfExceeded(Ip)} 로 관리</li>
 * </ul>
 *
 * <p>모든 쓰기·조회 메서드는 검증된 {@link Ip} 값 객체만 받으므로
 * 로그·Redis 키 인젝션이 타입 수준에서 차단된다.
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

    private final StringRedisTemplate stringRedisTemplate;
    private final MeterRegistry meterRegistry;
    private final IpBlockProperties properties;

    private Counter blockedRequestCounter;
    private Counter newIpBlockCounter;

    @PostConstruct
    private void initMetrics() {
        blockedRequestCounter = Counter.builder("ip.block.request.blocked.total")
                .description("IP 차단에 의해 거절된 요청 수")
                .register(meterRegistry);
        newIpBlockCounter = Counter.builder("ip.block.new.total")
                .description("새로 차단된 IP 수")
                .register(meterRegistry);
    }

    @CircuitBreaker(name = "redisBlockStore", fallbackMethod = "isBlockedFallback")
    public boolean isBlocked(Ip ip) {
        Boolean hasKey = stringRedisTemplate.hasKey(BLOCKED_IP_PREFIX + ip.value());
        boolean blocked = Boolean.TRUE.equals(hasKey);
        if (blocked) {
            blockedRequestCounter.increment();
        }
        return blocked;
    }

    private boolean isBlockedFallback(Ip ip, Throwable t) {
        log.warn("서킷 브레이커 OPEN/장애 발생: Redis 장애로 IP 차단 여부를 확인할 수 없습니다. Fail-open 처리합니다. ip={} error={}", ip, t.getMessage());
        return false; // Redis 장애 시 차단하지 않고 통과시킴
    }

    @CircuitBreaker(name = "redisBlockStore", fallbackMethod = "blockImmediatelyFallback")
    public void blockImmediately(Ip ip) {
        stringRedisTemplate.opsForValue().set(BLOCKED_IP_PREFIX + ip.value(), "1", properties.blockTtl());
        newIpBlockCounter.increment();
        log.warn("IP 차단 등록: ip={} ttl={}h", ip, properties.blockTtl().toHours());
    }

    private void blockImmediatelyFallback(Ip ip, Throwable t) {
        log.warn("서킷 브레이커 OPEN/장애 발생: Redis 장애로 IP를 차단할 수 없습니다. ip={} error={}", ip, t.getMessage());
    }

    /**
     * 404 카운터를 증가시키고, 임계값 이상이면 IP를 차단한다.
     * 카운터 키가 처음 생성될 때 TTL을 설정해 Fixed Window를 구현한다.
     */
    @CircuitBreaker(name = "redisBlockStore", fallbackMethod = "incrementNotFoundFallback")
    public void incrementNotFoundAndBlockIfExceeded(Ip ip) {
        final String key = NOT_FOUND_COUNTER_PREFIX + ip.value();
        final Long count = stringRedisTemplate.execute(
                INCREMENT_WITH_EXPIRE_SCRIPT,
                List.of(key),
                String.valueOf(properties.notFoundWindow().getSeconds())
        );

        if (count >= properties.notFoundThreshold()) {
            log.warn("404 임계값 초과 → IP 차단: ip={} count={}", ip, count);
            blockImmediately(ip);
        }
    }

    private void incrementNotFoundFallback(Ip ip, Throwable t) {
        log.warn("서킷 브레이커 OPEN/장애 발생: Redis 장애로 404 카운터를 처리할 수 없습니다. ip={} error={}", ip, t.getMessage());
    }

    @CircuitBreaker(name = "redisBlockStore", fallbackMethod = "unblockFallback")
    public void unblock(Ip ip) {
        stringRedisTemplate.delete(BLOCKED_IP_PREFIX + ip.value());
        stringRedisTemplate.delete(NOT_FOUND_COUNTER_PREFIX + ip.value());
        log.info("IP 차단 해제: ip={}", ip);
    }

    private void unblockFallback(Ip ip, Throwable t) {
        log.warn("서킷 브레이커 OPEN/장애 발생: Redis 장애로 IP 차단을 해제할 수 없습니다. ip={} error={}", ip, t.getMessage());
    }

    @CircuitBreaker(name = "redisBlockStore", fallbackMethod = "getBlockedIpsFallback")
    public List<BlockedIp> getBlockedIps() {
        final ScanOptions options = ScanOptions.scanOptions()
                .match(BLOCKED_IP_PREFIX + "*")
                .count(100L)
                .build();
        final Set<String> keys = stringRedisTemplate.execute(
                (RedisCallback<Set<String>>) connection -> {
                    final Set<String> result = new HashSet<>();
                    try (var cursor = connection.keyCommands().scan(options)) {
                        cursor.forEachRemaining(k -> result.add(new String(k, StandardCharsets.UTF_8)));
                    }
                    return result;
                });
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .map(key -> {
                    final String ip = key.substring(BLOCKED_IP_PREFIX.length());
                    final Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
                    return new BlockedIp(ip, ttl);
                })
                .sorted(Comparator.comparing(BlockedIp::ip))
                .toList();
    }

    private List<BlockedIp> getBlockedIpsFallback(Throwable t) {
        log.warn("서킷 브레이커 OPEN/장애 발생: Redis 장애로 차단 IP 목록을 조회할 수 없습니다. error={}", t.getMessage());
        return List.of();
    }

    public record BlockedIp(String ip, long remainingTtlSeconds) {
    }
}
