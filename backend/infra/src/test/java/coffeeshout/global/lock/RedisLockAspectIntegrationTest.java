package coffeeshout.global.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.InfraModuleIntegrationTest;
import coffeeshout.fixture.RedisLockedTaskFake;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * RedisLockAspect 통합 테스트.
 * <p>
 * Mockito 단위 테스트는 AOP 프록시를 우회하므로 애스펙트 동작을 검증할 수 없다.
 * 실제 Spring 컨텍스트 + Valkey TestContainer 위에서 락·done 마킹·이중 체크를 검증한다.
 */
@Import(RedisLockTestConfig.class)
class RedisLockAspectIntegrationTest extends InfraModuleIntegrationTest {

    @Autowired
    private RedisLockedTaskFake task;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired(required = false)
    private RedisLockAspect aspect;

    @Autowired(required = false)
    private org.redisson.api.RedissonClient redissonClient;

    @Test
    void 애스펙트_빈이_컨텍스트에_로드된다() {
        assertSoftly(softly -> {
            softly.assertThat(redissonClient).as("RedissonClient 빈").isNotNull();
            softly.assertThat(aspect).as("RedisLockAspect 빈").isNotNull();
        });
    }

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        task.reset();
        executor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    void tearDown() {
        task.releaseHeldExecution();
        executor.shutdownNow();
    }

    @Nested
    class 같은_키로_다시_호출되면 {

        @Test
        void 이미_처리된_이벤트는_실행하지_않고_null을_반환한다() {
            // given
            final String first = task.execute("evt-1");

            // when
            final String second = task.execute("evt-1");

            // then
            assertSoftly(softly -> {
                softly.assertThat(first).isEqualTo("evt-1");
                softly.assertThat(second).isNull();
                softly.assertThat(task.executionCount()).isEqualTo(1);
            });
        }

        @Test
        void 처리_완료_마킹이_TTL과_함께_저장된다() {
            // when
            task.execute("evt-2");

            // then
            final String doneKey = RedisLockedTaskFake.DONE_PREFIX + "evt-2";
            assertSoftly(softly -> {
                softly.assertThat(redisTemplate.hasKey(doneKey)).isTrue();
                softly.assertThat(redisTemplate.getExpire(doneKey, TimeUnit.MILLISECONDS)).isPositive();
            });
        }

        @Test
        void 다른_키는_서로_간섭하지_않고_각각_실행된다() {
            // when
            task.execute("evt-3");
            task.execute("evt-4");

            // then
            assertThat(task.executionCount()).isEqualTo(2);
        }
    }

    @Nested
    class 동시에_같은_키로_진입하면 {

        @Test
        void 락_보유_중이면_대기_없는_호출은_즉시_스킵된다() throws Exception {
            // given — 첫 스레드가 락을 보유한 채 메서드 안에서 대기
            task.holdNextExecution();
            final Future<String> holder = executor.submit(() -> task.execute("evt-5"));
            assertThat(task.awaitEntered(3, TimeUnit.SECONDS)).isTrue();

            // when — waitTime=0 이므로 락 획득 실패 즉시 포기
            final String contender = task.execute("evt-5");
            task.releaseHeldExecution();

            // then
            final String holderResult = holder.get(3, TimeUnit.SECONDS);
            assertSoftly(softly -> {
                softly.assertThat(contender).isNull();
                softly.assertThat(holderResult).isEqualTo("evt-5");
                softly.assertThat(task.executionCount()).isEqualTo(1);
            });
        }

        @Test
        void 락_대기_후_진입해도_이중_체크가_중복_실행을_차단한다() throws Exception {
            // given — 첫 스레드가 락을 보유한 채 메서드 안에서 대기
            task.holdNextExecution();
            final Future<String> holder = executor.submit(() -> task.execute("evt-6"));
            assertThat(task.awaitEntered(3, TimeUnit.SECONDS)).isTrue();

            // when — waitTime=3000 이므로 락이 풀릴 때까지 대기 후 진입,
            // 1차 체크는 이미 통과한 뒤라 락 획득 후 2차 체크(done)가 유일한 방어선이다
            final Future<String> waiter = executor.submit(() -> task.executeWithLockWait("evt-6"));
            task.releaseHeldExecution();

            // then
            final String holderResult = holder.get(3, TimeUnit.SECONDS);
            final String waiterResult = waiter.get(5, TimeUnit.SECONDS);
            assertSoftly(softly -> {
                softly.assertThat(holderResult).isEqualTo("evt-6");
                softly.assertThat(waiterResult).isNull();
                softly.assertThat(task.executionCount()).isEqualTo(1);
            });
        }
    }

    @Nested
    class 실행이_실패하면 {

        @Test
        void done_마킹이_없어_같은_키로_재시도할_수_있다() {
            // given — 첫 실행이 예외로 실패
            assertThatThrownBy(() -> task.executeFailing("evt-7"))
                    .isInstanceOf(IllegalStateException.class);

            // then — 실패한 실행은 done으로 마킹되지 않는다 (재시도 여지 보존)
            final boolean markedAfterFailure = redisTemplate.hasKey(RedisLockedTaskFake.DONE_PREFIX + "evt-7");

            // when — 같은 키로 재시도하면 실행된다
            final String retry = task.execute("evt-7");

            // then
            assertSoftly(softly -> {
                softly.assertThat(markedAfterFailure).isFalse();
                softly.assertThat(retry).isEqualTo("evt-7");
                softly.assertThat(task.executionCount()).isEqualTo(2);
            });
        }
    }
}
