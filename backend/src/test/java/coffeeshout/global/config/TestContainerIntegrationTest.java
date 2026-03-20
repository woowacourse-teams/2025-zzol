package coffeeshout.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.test.IntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

@IntegrationTest
@DisplayName("TestContainer 통합 테스트")
class TestContainerIntegrationTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("TestContainer Valkey 연결 확인")
    void testValkeyConnectionIsAvailable() {
        // given & when
        Boolean isContainerRunning = TestContainerConfig.isContainerRunning();
        String host = TestContainerConfig.getContainerHost();
        Integer port = TestContainerConfig.getContainerPort();

        // then
        assertThat(isContainerRunning).isTrue();
        assertThat(host).isNotNull();
        assertThat(port).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("Redis String 연산 테스트")
    void testRedisStringOperations() {
        // given
        String key = "test:string:key";
        String value = "test-value";

        // when
        stringRedisTemplate.opsForValue().set(key, value);
        String retrievedValue = stringRedisTemplate.opsForValue().get(key);

        // then
        assertThat(retrievedValue).isEqualTo(value);
    }

    @Test
    @DisplayName("Redis String TTL 설정 테스트")
    void testRedisStringWithTtl() {
        // given
        String key = "test:ttl:key";
        String value = "test-ttl-value";
        Duration ttl = Duration.ofSeconds(10);

        // when
        stringRedisTemplate.opsForValue().set(key, value, ttl);
        Long remainingTtl = stringRedisTemplate.getExpire(key);

        // then
        assertThat(remainingTtl).isGreaterThan(0).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Redis Hash 연산 테스트")
    void testRedisHashOperations() {
        // given
        String key = "test:hash:key";
        String hashKey = "field1";
        String hashValue = "value1";

        // when
        stringRedisTemplate.opsForHash().put(key, hashKey, hashValue);
        Object retrievedValue = stringRedisTemplate.opsForHash().get(key, hashKey);

        // then
        assertThat(retrievedValue).isEqualTo(hashValue);
    }

    @Test
    @DisplayName("Redis List 연산 테스트")
    void testRedisListOperations() {
        // given
        String key = "test:list:key";
        String value1 = "item1";
        String value2 = "item2";

        // when
        stringRedisTemplate.opsForList().rightPush(key, value1);
        stringRedisTemplate.opsForList().rightPush(key, value2);
        String poppedValue = stringRedisTemplate.opsForList().leftPop(key);

        // then
        assertThat(poppedValue).isEqualTo(value1);
        assertThat(stringRedisTemplate.opsForList().size(key)).isEqualTo(1);
    }

    @Test
    @DisplayName("Redis Set 연산 테스트")
    void testRedisSetOperations() {
        // given
        String key = "test:set:key";
        String member1 = "member1";
        String member2 = "member2";

        // when
        stringRedisTemplate.opsForSet().add(key, member1, member2);
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, member1);
        Long setSize = stringRedisTemplate.opsForSet().size(key);

        // then
        assertThat(isMember).isTrue();
        assertThat(setSize).isEqualTo(2);
    }

    @Test
    @DisplayName("Redis 트랜잭션 테스트")
    void testRedisTransaction() {
        // given
        String key1 = "test:tx:key1";
        String key2 = "test:tx:key2";
        String value1 = "value1";
        String value2 = "value2";

        // when
        stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.multi();
            connection.stringCommands().set(key1.getBytes(), value1.getBytes());
            connection.stringCommands().set(key2.getBytes(), value2.getBytes());
            connection.exec();
            return null;
        });

        // then
        String retrievedValue1 = stringRedisTemplate.opsForValue().get(key1);
        String retrievedValue2 = stringRedisTemplate.opsForValue().get(key2);

        assertThat(retrievedValue1).isEqualTo(value1);
        assertThat(retrievedValue2).isEqualTo(value2);
    }

    @Test
    @DisplayName("Redis 연결 풀 테스트")
    void testRedisConnectionPool() {
        // given
        String keyPrefix = "test:pool:key:";
        int numberOfOperations = 50;

        // when & then
        for (int i = 0; i < numberOfOperations; i++) {
            String key = keyPrefix + i;
            String value = "value" + i;

            stringRedisTemplate.opsForValue().set(key, value);
            String retrievedValue = stringRedisTemplate.opsForValue().get(key);

            assertThat(retrievedValue).isEqualTo(value);
        }
    }
}
