package coffeeshout.global.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Redis 분산 락을 사용하는 메서드에 적용하는 어노테이션
 * <p>
 * 사용 예시:
 *
 * @RedisLock( key = "#event.eventId()", lockPrefix = "event:lock:", donePrefix = "event:done:", waitTime = 0, leaseTime
 * = 5000 ) void handle(RouletteSpinEvent event) { // 비즈니스 로직 }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {

    /**
     * 락 키를 생성하기 위한 SpEL 표현식 예: "#event.eventId()", "#joinCode"
     */
    String key();

    /**
     * 락 키 접두사 기본값: "lock:"
     */
    String lockPrefix() default "lock:";

    /**
     * 처리 완료 마킹용 키 접두사 기본값: "done:"
     */
    String donePrefix() default "done:";

    /**
     * 락 획득 대기 시간 (밀리초) 0이면 즉시 포기 기본값: 0
     */
    long waitTime() default 0;

    /**
     * 락 유지 시간 (밀리초) 기본값: 5000 (5초)
     */
    long leaseTime() default 5000;

    /**
     * 처리 완료 마킹 TTL (밀리초) 기본값: 600000 (10분)
     */
    long doneTtl() default 600000;
}
