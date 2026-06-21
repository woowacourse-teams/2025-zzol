package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.application.LlmCallBudget;
import coffeeshout.zzolbot.monitor.config.LlmBudgetProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 원자 INCR 기반 일일 예산. 키는 날짜별이며 TTL로 자동 리셋된다.
 * 다중 인스턴스가 같은 Redis 카운터를 공유하므로 합산 총량이 캡을 절대 넘지 않는다.
 */
@RequiredArgsConstructor
public class RedisLlmCallBudget implements LlmCallBudget {

    private static final String KEY_PREFIX = "zzolbot:llm-budget:";

    private final StringRedisTemplate redisTemplate;
    private final LlmBudgetProperties properties;
    private final Clock clock;

    @Override
    public boolean tryAcquire() {
        final String key = todayKey();
        final Long current = redisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redisTemplate.expire(key, Duration.ofHours(26));
        }
        return current == null || current <= properties.dailyMax();
    }

    @Override
    public long remaining() {
        final String value = redisTemplate.opsForValue().get(todayKey());
        final long used = value == null ? 0 : Long.parseLong(value);
        return Math.max(0, properties.dailyMax() - used);
    }

    private String todayKey() {
        return KEY_PREFIX + LocalDate.now(clock);
    }
}
