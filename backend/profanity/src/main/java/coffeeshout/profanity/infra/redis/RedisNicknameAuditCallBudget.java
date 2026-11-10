package coffeeshout.profanity.infra.redis;

import coffeeshout.profanity.application.port.NicknameAuditCallBudget;
import coffeeshout.profanity.config.NicknameAuditProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 날짜별 Redis 카운터로 센 하루 호출 예산.
 *
 * <p>카운터를 Redis에 두는 이유는 Blue/Green 전환 구간에 컨테이너 둘이 함께 살아 있어서다. JVM 안에서 세면
 * 재배포할 때마다 0으로 돌아가고, 두 인스턴스가 각자 상한만큼 쓴다.
 *
 * <p>Redis가 응답하지 않으면 예외가 그대로 올라가 호출이 나가지 않는다. 세지 못한 호출을 내보내면 상한이
 * 의미가 없다. 회차 락도 같은 Redis에 있어서 그 상황에서는 회차 자체가 시작되지 않는다.
 *
 * <p>프로파일은 이 예산을 쓰는 {@code GeminiNicknameAuditor}와 같게 둔다. local·test의 스텁 검열기는
 * Gemini를 부르지 않으므로 셀 것이 없다.
 */
@Slf4j
@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class RedisNicknameAuditCallBudget implements NicknameAuditCallBudget {

    /**
     * Gemini 무료 티어의 하루 한도는 태평양 시간 자정에 초기화된다. 같은 경계로 세지 않으면 상한과 한도가
     * 서로 다른 24시간을 봐서, 한도가 풀린 뒤에도 막거나 한도가 찬 뒤에도 내보낸다.
     */
    private static final ZoneId QUOTA_RESET_ZONE = ZoneId.of("America/Los_Angeles");

    private static final String KEY_PREFIX = "nickname-audit:gemini-calls:";

    /** 날짜가 바뀌면 그 키는 다시 읽지 않는다. 서머타임이 끝나는 날은 하루가 25시간이라 하루보다 길게 둔다. */
    private static final Duration KEY_TTL = Duration.ofDays(2);

    private final StringRedisTemplate redisTemplate;
    private final NicknameAuditProperties properties;
    private final Clock clock;

    @Override
    public boolean tryAcquire() {
        final String key = KEY_PREFIX + quotaDay();
        final Long used = redisTemplate.opsForValue().increment(key);
        if (used == null) {
            throw new IllegalStateException("닉네임 검열 호출 수를 세지 못했습니다. key=" + key);
        }
        if (used == 1L) {
            redisTemplate.expire(key, KEY_TTL);
        }
        final int limit = properties.dailyCallLimit();
        if (used == limit + 1L) {
            // INCR이 원자적이라 이 값을 받는 호출은 인스턴스를 통틀어 하루에 하나다. 이후 거절은 메트릭으로만 남는다.
            log.warn("닉네임 검열 하루 호출 상한 {}회에 닿았다. 태평양 시간 자정까지 Gemini를 부르지 않고 미검열 행은 그대로 둔다.", limit);
        }
        return used <= limit;
    }

    private LocalDate quotaDay() {
        return clock.instant().atZone(QUOTA_RESET_ZONE).toLocalDate();
    }
}
