package coffeeshout.room.infra;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.repository.JoinCodeRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisJoinCodeRepository implements JoinCodeRepository {

    private static final String JOIN_CODE_KEY_PREFIX = "room:joinCode:";

    @Value("${room.removalDelay}")
    private Duration ttl;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean save(JoinCode joinCode) {
        final String key = JOIN_CODE_KEY_PREFIX + joinCode.getValue();
        // SETNX 명령어로 원자적으로 저장 (키가 없을 때만 저장)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(success);
    }
}
