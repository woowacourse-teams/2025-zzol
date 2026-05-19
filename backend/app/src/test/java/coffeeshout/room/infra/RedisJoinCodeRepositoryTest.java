package coffeeshout.room.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.ServiceTest;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

class RedisJoinCodeRepositoryTest extends ServiceTest {

    @Autowired
    JoinCodeGenerator joinCodeGenerator;

    @Autowired
    RedisJoinCodeRepository redisJoinCodeRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private static final String JOIN_CODE_KEY_PREFIX = "room:joinCode:";

    JoinCode joinCode = new JoinCode("ABCD");

    @AfterEach
    void tearDown() {
        // 각 테스트 후 Redis 데이터 정리 - 패턴 매칭으로 모든 조인코드 키 삭제
        Set<String> keys = redisTemplate.keys(JOIN_CODE_KEY_PREFIX + "*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void 조인코드를_저장한다() {
        // given
        // when
        boolean saved = redisJoinCodeRepository.save(joinCode);

        // then
        assertThat(saved).isTrue();
        String key = JOIN_CODE_KEY_PREFIX + joinCode.getValue();
        Boolean hasKey = redisTemplate.hasKey(key);
        assertThat(hasKey).isTrue();
    }

    @Test
    void 중복된_조인코드를_저장하면_실패한다() {
        // given
        boolean firstSave = redisJoinCodeRepository.save(joinCode);
        assertThat(firstSave).isTrue();

        // when - 동일한 조인코드로 다시 저장 시도
        boolean secondSave = redisJoinCodeRepository.save(joinCode);

        // then - 실패해야 함 (SETNX는 키가 이미 존재하면 false 반환)
        assertThat(secondSave).isFalse();
    }

    @Test
    void 각_조인코드마다_개별_TTL이_설정된다() {
        // given
        JoinCode joinCode1 = joinCodeGenerator.generate();
        JoinCode joinCode2 = joinCodeGenerator.generate();

        // when - generate()가 이미 저장까지 함

        // then - 각 키마다 TTL이 설정되어 있어야 함
        String key1 = JOIN_CODE_KEY_PREFIX + joinCode1.getValue();
        String key2 = JOIN_CODE_KEY_PREFIX + joinCode2.getValue();

        Long ttl1 = redisTemplate.getExpire(key1);
        Long ttl2 = redisTemplate.getExpire(key2);

        assertThat(ttl1).isNotNull().isGreaterThan(0);
        assertThat(ttl2).isNotNull().isGreaterThan(0);
    }

    @Test
    void 저장_시_TTL이_설정된다() {
        // given

        // when
        redisJoinCodeRepository.save(joinCode);

        // then
        String key = JOIN_CODE_KEY_PREFIX + joinCode.getValue();
        Long ttl = redisTemplate.getExpire(key);
        assertThat(ttl).isNotNull()
                .isGreaterThan(0);
    }

    @Test
    void SETNX로_원자성이_보장된다() {
        // given

        // when - 동시에 저장 시도하는 것을 시뮬레이션
        boolean firstSave = redisJoinCodeRepository.save(joinCode);
        boolean secondSave = redisJoinCodeRepository.save(joinCode);

        // then - 하나만 성공해야 함
        assertThat(firstSave).isTrue();
        assertThat(secondSave).isFalse();
    }

    @Test
    void 동시에_여러_스레드가_같은_코드를_저장하면_하나만_성공한다() throws Exception {
        // given

        int threadCount = 10;

        // when - 10개의 스레드가 동시에 같은 코드를 저장 시도
        List<CompletableFuture<Boolean>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.supplyAsync(
                        () -> redisJoinCodeRepository.save(joinCode)
                ))
                .toList();

        // 모든 작업 완료 대기
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.get();

        // then - 성공한 개수는 정확히 1개여야 함
        long successCount = futures.stream()
                .map(CompletableFuture::join)
                .filter(result -> result)
                .count();

        assertThat(successCount).isEqualTo(1);
    }
}
