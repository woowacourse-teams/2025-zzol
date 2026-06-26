package coffeeshout.zzolbot.remediation.infra;

import coffeeshout.zzolbot.remediation.application.RemediationBudget;
import coffeeshout.zzolbot.remediation.config.RemediationProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 원자 INCR 기반 일일 디스패치 예산. 키는 날짜별이며 TTL로 자동 리셋된다.
 * 다중 인스턴스가 같은 Redis 카운터를 공유하므로 합산 총량이 캡을 절대 넘지 않는다.
 */
@RequiredArgsConstructor
public class RedisRemediationBudget implements RemediationBudget {

    private static final String KEY_PREFIX = "zzolbot:remediation-budget:";
    private static final long NO_EXPIRY = -1L;
    private static final Duration TTL = Duration.ofHours(26);

    private final StringRedisTemplate redisTemplate;
    private final RemediationProperties properties;
    private final Clock clock;

    @Override
    public boolean tryAcquire() {
        final String key = todayKey();
        final Long current = redisTemplate.opsForValue().increment(key);
        if (current == null) {
            // Redis 비정상 — 예산으로 가용성을 막지 않는다(쿨다운·화이트리스트가 1차 방어).
            return true;
        }
        ensureExpiry(key);
        return current <= properties.dailyMax();
    }

    @Override
    public long remaining() {
        final String value = redisTemplate.opsForValue().get(todayKey());
        return Math.max(0, properties.dailyMax() - parseUsed(value));
    }

    /**
     * INCR이 만든 값은 정상이면 숫자지만, 키가 손상·수동변경됐을 수 있어 방어적으로 파싱한다.
     * 파싱 실패 시 0으로 보아 가용성을 막지 않는다(예산은 INCR(tryAcquire)에서 1차로 강제된다).
     */
    private long parseUsed(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * INCR과 EXPIRE는 원자가 아니라 EXPIRE 단독 실패 시 키에 TTL이 영영 없을 수 있다.
     * 매 호출 TTL을 확인해 누락이면 다시 건다(키가 영구 잠금되는 것을 방지).
     */
    private void ensureExpiry(String key) {
        final Long ttl = redisTemplate.getExpire(key);
        if (ttl != null && ttl == NO_EXPIRY) {
            redisTemplate.expire(key, TTL);
        }
    }

    private String todayKey() {
        return KEY_PREFIX + LocalDate.now(clock);
    }
}
