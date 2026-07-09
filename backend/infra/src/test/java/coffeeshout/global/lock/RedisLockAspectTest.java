package coffeeshout.global.lock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * RedisLockAspect 선언부 회귀 가드.
 * <p>
 * 애스펙트가 @Transactional(기본 Ordered.LOWEST_PRECEDENCE)보다 먼저 실행되어야
 * 중복 이벤트가 트랜잭션(DB 커넥션) 획득 전에 걸러진다. 이 순서가 바뀌면
 * 기능은 그대로 동작하는 것처럼 보이지만 커넥션 낭비 방지 효과가 사라진다.
 */
class RedisLockAspectTest {

    @Test
    void 트랜잭션보다_먼저_실행되도록_최우선_순위로_선언되어_있다() {
        final Order order = RedisLockAspect.class.getAnnotation(Order.class);

        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
