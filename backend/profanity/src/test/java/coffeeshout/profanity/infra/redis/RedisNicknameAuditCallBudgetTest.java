package coffeeshout.profanity.infra.redis;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.fixture.NicknameAuditPropertiesFixture;
import coffeeshout.support.ServiceTest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 하루 호출 상한을 실제 Valkey로 검증한다(#1907).
 *
 * <p>상한은 인스턴스 사이에서 합쳐져야 하고 날짜 경계는 Gemini 한도가 초기화되는 태평양 시간 자정이어야 한다.
 * 둘 다 Redis 키가 정하는 동작이라 목으로는 확인되지 않는다.
 */
class RedisNicknameAuditCallBudgetTest extends ServiceTest {

    private static final int 상한 = 3;

    /** 태평양 표준시(UTC-8)로 2026-11-10 23:59:00. */
    private static final Instant 태평양_자정_1분_전 = Instant.parse("2026-11-11T07:59:00Z");

    /** 태평양 표준시로 2026-11-11 00:01:00. UTC 날짜는 위와 같은 11일이다. */
    private static final Instant 태평양_자정_1분_후 = Instant.parse("2026-11-11T08:01:00Z");

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void 상한까지는_허용하고_넘으면_막는다() {
        final RedisNicknameAuditCallBudget budget = budgetAt(태평양_자정_1분_전);

        for (int call = 1; call <= 상한; call++) {
            assertThat(budget.tryAcquire()).as("%d번째 호출", call).isTrue();
        }
        assertThat(budget.tryAcquire()).as("상한을 넘긴 호출").isFalse();
    }

    @Test
    void 여러_인스턴스가_같은_횟수를_나눠_쓴다() {
        // Blue/Green 전환 구간에는 컨테이너 둘이 같은 Valkey를 본다. 인스턴스마다 따로 세면 상한이 두 배가 된다.
        final RedisNicknameAuditCallBudget blue = budgetAt(태평양_자정_1분_전);
        final RedisNicknameAuditCallBudget green = budgetAt(태평양_자정_1분_전);

        assertThat(blue.tryAcquire()).isTrue();
        assertThat(green.tryAcquire()).isTrue();
        assertThat(blue.tryAcquire()).isTrue();

        assertThat(green.tryAcquire()).as("두 인스턴스의 호출을 합쳐 상한에 닿았다.").isFalse();
    }

    @Test
    void 태평양_시간_자정이_지나면_횟수가_초기화된다() {
        final RedisNicknameAuditCallBudget beforeMidnight = budgetAt(태평양_자정_1분_전);
        for (int call = 0; call < 상한; call++) {
            beforeMidnight.tryAcquire();
        }
        assertThat(beforeMidnight.tryAcquire()).isFalse();

        final RedisNicknameAuditCallBudget afterMidnight = budgetAt(태평양_자정_1분_후);

        assertThat(afterMidnight.tryAcquire())
                .as("두 시각은 UTC로 같은 날이다. UTC 날짜로 세면 한도가 초기화된 뒤에도 계속 막힌다.")
                .isTrue();
    }

    @Test
    void 날짜별_키는_만료_시간을_갖는다() {
        budgetAt(태평양_자정_1분_전).tryAcquire();

        final Set<String> keys = redisTemplate.keys("nickname-audit:gemini-calls:*");

        assertThat(keys).containsExactly("nickname-audit:gemini-calls:2026-11-10");
        assertThat(redisTemplate.getExpire("nickname-audit:gemini-calls:2026-11-10"))
                .as("만료가 없으면 날짜별 키가 하루에 하나씩 영영 쌓인다.")
                .isPositive();
    }

    private RedisNicknameAuditCallBudget budgetAt(Instant now) {
        return new RedisNicknameAuditCallBudget(
                redisTemplate, NicknameAuditPropertiesFixture.하루_호출_상한(상한), Clock.fixed(now, ZoneOffset.UTC));
    }
}
