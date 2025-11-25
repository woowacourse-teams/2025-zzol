package coffeeshout.global.lock;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Redis 분산 락 AOP (Redisson 사용)
 *
 * @RedisLock 어노테이션이 붙은 메서드 실행 전에 락을 획득하고, 실행 후 락을 해제함
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 트랜잭션보다 먼저 실행
@RequiredArgsConstructor
@ConditionalOnBean(RedissonClient.class)
public class RedisLockAspect {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(coffeeshout.global.lock.RedisLock)")
    public Object lock(ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Method method = signature.getMethod();
        final RedisLock redisLock = method.getAnnotation(RedisLock.class);
        
        final String lockKey = getLockKey(joinPoint, redisLock);
        final String doneKey = getDoneKey(joinPoint, redisLock);

        // 이미 처리된 이벤트인지 확인
        if (isAlreadyProcessed(doneKey)) {
            log.debug("이미 처리된 이벤트 (스킵): doneKey={}", doneKey);
            return null;
        }

        // Redisson 락 객체 가져오기
        final RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도
            final boolean acquired = lock.tryLock(
                    redisLock.waitTime(), 
                    redisLock.leaseTime(), 
                    TimeUnit.MILLISECONDS
            );

            if (!acquired) {
                log.warn("락 획득 실패 (스킵): lockKey={}", lockKey);
                return null;
            }

            // 메서드 실행
            log.debug("락 획득 성공, 메서드 실행: lockKey={}", lockKey);
            final Object result = joinPoint.proceed();

            // 처리 완료 마킹
            markAsDone(doneKey, redisLock.doneTtl());
            log.debug("처리 완료 마킹: doneKey={}", doneKey);

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("락 획득 중 인터럽트 발생: lockKey={}", lockKey, e);
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        } finally {
            // 락 해제 (현재 스레드가 락을 보유한 경우에만)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("락 해제: lockKey={}", lockKey);
            }
        }
    }

    private String getLockKey(ProceedingJoinPoint joinPoint, RedisLock redisLock) {
        final String dynamicKey = parseSpel(joinPoint, redisLock.key());
        return redisLock.lockPrefix() + dynamicKey;
    }

    private String getDoneKey(ProceedingJoinPoint joinPoint, RedisLock redisLock) {
        final String dynamicKey = parseSpel(joinPoint, redisLock.key());
        return redisLock.donePrefix() + dynamicKey;
    }

    private String parseSpel(ProceedingJoinPoint joinPoint, String spelExpression) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Method method = signature.getMethod();
        final Object[] args = joinPoint.getArgs();
        final String[] parameterNames = signature.getParameterNames();

        final StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        return parser.parseExpression(spelExpression).getValue(context, String.class);
    }

    private boolean isAlreadyProcessed(String doneKey) {
        return redisTemplate.hasKey(doneKey);
    }

    private void markAsDone(String doneKey, long doneTtl) {
        redisTemplate.opsForValue()
                .set(doneKey, "done", Duration.ofMillis(doneTtl));
    }
}
