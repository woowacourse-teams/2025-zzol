package coffeeshout.global.lock;

import coffeeshout.fixture.RedisLockedTaskFake;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @RedisLock이 붙은 락 대상 빈을 컨텍스트에 등록한다.
 * 부모 클래스에 @SpringBootTest가 선언된 구조에서는 테스트 클래스 내부 @TestConfiguration이
 * 자동 감지되지 않으므로 독립 파일로 분리한다 (docs/conventions-test.md).
 */
@TestConfiguration
public class RedisLockTestConfig {

    @Bean
    public RedisLockedTaskFake redisLockedTaskFake() {
        return new RedisLockedTaskFake();
    }
}
